package br.senac.recursos.integracao;

import br.senac.recursos.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-30: as regras que protegem os limitadores de nota valem no próprio banco (arquitetura, seção 3.2),
 * mesmo que o código Java erre. Por isso estes testes falam SQL direto com um PostgreSQL real
 * (Testcontainers), sem passar pela aplicação, e conferem o SQLState de cada recusa.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RestricoesDoBancoTest {

    private static final String EXCLUSAO = "23P01";
    private static final String CHECK = "23514";
    private static final String UNICA = "23505";
    private static final String CHAVE_ESTRANGEIRA = "23503";

    @Autowired
    JdbcTemplate jdbc;

    long solicitante;
    long sala;

    @BeforeEach
    void limparEPreparar() {
        jdbc.execute("ALTER TABLE auditoria DISABLE TRIGGER auditoria_imutavel");
        jdbc.execute("TRUNCATE auditoria, conflito_evitado, retirada, periodo_manutencao, reserva_recurso, reserva, "
                + "competencia, sala, professor, material, recurso, usuario RESTART IDENTITY CASCADE");
        jdbc.execute("ALTER TABLE auditoria ENABLE TRIGGER auditoria_imutavel");
        solicitante = inserirUsuario("marina@senac.br", "SOLICITANTE");
        sala = inserirSala("sala-101");
    }

    @Nested
    @DisplayName("Sobreposição de reservas (RN-02, RN-03, S-01, S-02)")
    class Sobreposicao {

        @Test
        void mesmoRecursoNoMesmoHorarioERecusado() {
            reservar("2026-11-10T10:00Z", "2026-11-10T11:00Z", "SOLICITADA", sala);

            assertThatThrownBy(() -> reservar("2026-11-10T10:30Z", "2026-11-10T11:30Z", "SOLICITADA", sala))
                    .satisfies(e -> assertThat(sqlState(e)).isEqualTo(EXCLUSAO));
            assertThat(contar("reserva_recurso")).isEqualTo(1);
        }

        @Test
        void reservasEncostadasConvivem() {
            reservar("2026-11-10T10:00Z", "2026-11-10T11:00Z", "SOLICITADA", sala);
            reservar("2026-11-10T11:00Z", "2026-11-10T12:00Z", "SOLICITADA", sala);
            reservar("2026-11-10T09:00Z", "2026-11-10T10:00Z", "APROVADA", sala);

            assertThat(contar("reserva_recurso")).isEqualTo(3);
        }

        @ParameterizedTest(name = "reserva {0} não ocupa o horário")
        @ValueSource(strings = {"REJEITADA", "CANCELADA", "NAO_COMPARECEU", "CONCLUIDA"})
        void estadosQueNaoOcupamLiberamOHorario(String estado) {
            reservar("2026-11-10T10:00Z", "2026-11-10T11:00Z", estado, sala);
            reservar("2026-11-10T10:00Z", "2026-11-10T11:00Z", "SOLICITADA", sala);

            assertThat(contar("reserva_recurso")).isEqualTo(2);
        }

        @ParameterizedTest(name = "reserva {0} ocupa o horário")
        @ValueSource(strings = {"SOLICITADA", "APROVADA", "EM_USO"})
        void estadosQueOcupamBloqueiamOHorario(String estado) {
            reservar("2026-11-10T10:00Z", "2026-11-10T11:00Z", estado, sala);

            assertThatThrownBy(() -> reservar("2026-11-10T10:59Z", "2026-11-10T11:30Z", "SOLICITADA", sala))
                    .satisfies(e -> assertThat(sqlState(e)).isEqualTo(EXCLUSAO));
        }

        @Test
        void agendaDoProfessorTambemNaoSobrepoe() {
            long professor = inserirRecurso("PROFESSOR", "renata-silva-12345");
            long outraSala = inserirSala("sala-102");
            reservar("2026-11-10T10:00Z", "2026-11-10T11:00Z", "SOLICITADA", sala, professor);

            assertThatThrownBy(() -> reservar("2026-11-10T10:00Z", "2026-11-10T11:00Z", "SOLICITADA", outraSala, professor))
                    .satisfies(e -> assertThat(sqlState(e)).isEqualTo(EXCLUSAO));
            assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva_recurso WHERE recurso_id = ?", Integer.class, professor))
                    .isEqualTo(1);
        }

        @Test
        void cancelarAReservaLiberaOHorarioPeloGatilho() {
            long reserva = reservar("2026-11-10T10:00Z", "2026-11-10T11:00Z", "APROVADA", sala);

            jdbc.update("UPDATE reserva SET estado = 'CANCELADA' WHERE id = ?", reserva);

            assertThat(jdbc.queryForObject("SELECT ocupa FROM reserva_recurso WHERE reserva_id = ?", Boolean.class, reserva)).isFalse();
            reservar("2026-11-10T10:00Z", "2026-11-10T11:00Z", "SOLICITADA", sala);
        }

        @Test
        void alterarOHorarioParaCimaDeOutraReservaERecusado() {
            reservar("2026-11-10T10:00Z", "2026-11-10T11:00Z", "SOLICITADA", sala);
            long outra = reservar("2026-11-10T12:00Z", "2026-11-10T13:00Z", "SOLICITADA", sala);

            assertThatThrownBy(() -> jdbc.update(
                    "UPDATE reserva SET inicio = '2026-11-10T10:30Z', fim = '2026-11-10T11:30Z' WHERE id = ?", outra))
                    .satisfies(e -> assertThat(sqlState(e)).isEqualTo(EXCLUSAO));
        }
    }

    @Nested
    @DisplayName("Intervalo e estados (RN-01, restrição 3)")
    class IntervaloEEstados {

        @Test
        void terminoIgualAoInicioERecusado() {
            assertThatThrownBy(() -> reservar("2026-11-10T10:00Z", "2026-11-10T10:00Z", "SOLICITADA", sala))
                    .satisfies(e -> assertThat(sqlState(e)).isEqualTo(CHECK));
        }

        @Test
        void estadoForaDaListaERecusado() {
            assertThatThrownBy(() -> reservar("2026-11-10T10:00Z", "2026-11-10T11:00Z", "PENDENTE", sala))
                    .satisfies(e -> assertThat(sqlState(e)).isEqualTo(CHECK));
        }

        @Test
        void perfilForaDaListaERecusado() {
            assertThatThrownBy(() -> inserirUsuario("x@senac.br", "GERENTE"))
                    .satisfies(e -> assertThat(sqlState(e)).isEqualTo(CHECK));
        }
    }

    @Nested
    @DisplayName("Cadastros (RN-16, RN-17, restrições 8, 9, 12 e 13)")
    class Cadastros {

        @ParameterizedTest(name = "nome de sala inválido: \"{0}\"")
        @ValueSource(strings = {"Sala-01", "sala 01", "sala-1", "sala-1000", "lab-01", " sala-01"})
        void nomeDeSalaForaDoPadraoERecusado(String nome) {
            assertThatThrownBy(() -> inserirSala(nome)).satisfies(e -> assertThat(sqlState(e)).isEqualTo(CHECK));
        }

        @ParameterizedTest(name = "nome de professor inválido: \"{0}\"")
        @ValueSource(strings = {"Renata-silva-12345", "renata-silva-1234", "renata-maria-silva-12345", "renata-silva"})
        void nomeDeProfessorForaDoPadraoERecusado(String nome) {
            assertThatThrownBy(() -> inserirRecurso("PROFESSOR", nome)).satisfies(e -> assertThat(sqlState(e)).isEqualTo(CHECK));
        }

        @Test
        void nomeRepetidoNoMesmoTipoSemDiferenciarMaiusculasERecusado() {
            inserirRecurso("MATERIAL", "Projetor Epson");

            assertThatThrownBy(() -> inserirRecurso("MATERIAL", "projetor epson"))
                    .satisfies(e -> assertThat(sqlState(e)).isEqualTo(UNICA));
        }

        @Test
        void patrimonioRepetidoSemDiferenciarMaiusculasERecusado() {
            long a = inserirRecurso("MATERIAL", "Projetor A");
            long b = inserirRecurso("MATERIAL", "Projetor B");
            jdbc.update("INSERT INTO material (recurso_id, tipo_material, patrimonio) VALUES (?, 'Projetor', 'PAT-1')", a);

            assertThatThrownBy(() -> jdbc.update(
                    "INSERT INTO material (recurso_id, tipo_material, patrimonio) VALUES (?, 'Projetor', 'pat-1')", b))
                    .satisfies(e -> assertThat(sqlState(e)).isEqualTo(UNICA));
        }

        @Test
        void capacidadeZeroERecusada() {
            long recurso = inserirRecurso("SALA", "sala-200");

            assertThatThrownBy(() -> jdbc.update(
                    "INSERT INTO sala (recurso_id, capacidade, localizacao, tipo_sala) VALUES (?, 0, 'Bloco A', 'Aula')", recurso))
                    .satisfies(e -> assertThat(sqlState(e)).isEqualTo(CHECK));
        }

        @Test
        void recursoComReservaNaoPodeSerExcluido() {
            long projetor = inserirRecurso("MATERIAL", "Projetor Epson");
            reservar("2026-11-10T10:00Z", "2026-11-10T11:00Z", "CONCLUIDA", projetor);

            assertThatThrownBy(() -> jdbc.update("DELETE FROM recurso WHERE id = ?", projetor))
                    .satisfies(e -> assertThat(sqlState(e)).isEqualTo(CHAVE_ESTRANGEIRA));
        }
    }

    @Nested
    @DisplayName("Manutenção, retirada e auditoria (restrições 4, 6 e 7)")
    class ManutencaoRetiradaAuditoria {

        @Test
        void manutencoesDoMesmoRecursoNaoSeSobrepoem() {
            manutencao("2026-11-10T10:00Z", "2026-11-10T12:00Z");

            assertThatThrownBy(() -> manutencao("2026-11-10T11:00Z", "2026-11-10T13:00Z"))
                    .satisfies(e -> assertThat(sqlState(e)).isEqualTo(EXCLUSAO));
            manutencao("2026-11-10T12:00Z", "2026-11-10T13:00Z");
        }

        @Test
        void devolucaoAntesDaRetiradaERecusada() {
            long reserva = reservar("2026-11-10T10:00Z", "2026-11-10T11:00Z", "EM_USO", sala);
            long vinculo = jdbc.queryForObject("SELECT id FROM reserva_recurso WHERE reserva_id = ?", Long.class, reserva);

            assertThatThrownBy(() -> jdbc.update("INSERT INTO retirada (reserva_recurso_id, registrada_por, retirada_em, devolvida_em) "
                    + "VALUES (?, ?, '2026-11-10T10:00Z', '2026-11-10T09:59Z')", vinculo, solicitante))
                    .satisfies(e -> assertThat(sqlState(e)).isEqualTo(CHECK));
        }

        @Test
        void auditoriaNaoPodeSerAlteradaNemApagada() {
            long reserva = reservar("2026-11-10T10:00Z", "2026-11-10T11:00Z", "SOLICITADA", sala);
            jdbc.update("INSERT INTO auditoria (reserva_id, usuario_id, estado_novo, ocorrida_em) VALUES (?, ?, 'SOLICITADA', now())",
                    reserva, solicitante);

            assertThatThrownBy(() -> jdbc.update("UPDATE auditoria SET estado_novo = 'APROVADA'"))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> jdbc.update("DELETE FROM auditoria"))
                    .isInstanceOf(DataAccessException.class);
            assertThat(contar("auditoria")).isEqualTo(1);
        }
    }

    // ---------- apoio ----------

    private long inserirUsuario(String email, String perfil) {
        return jdbc.queryForObject("INSERT INTO usuario (nome, email, senha_hash, perfil) VALUES ('Teste', ?, 'hash', ?) RETURNING id",
                Long.class, email, perfil);
    }

    private long inserirRecurso(String tipo, String nome) {
        return jdbc.queryForObject("INSERT INTO recurso (tipo, nome) VALUES (?, ?) RETURNING id", Long.class, tipo, nome);
    }

    private long inserirSala(String nome) {
        long id = inserirRecurso("SALA", nome);
        jdbc.update("INSERT INTO sala (recurso_id, capacidade, localizacao, tipo_sala) VALUES (?, 30, 'Bloco A', 'Aula')", id);
        return id;
    }

    private long reservar(String inicio, String fim, String estado, long... recursos) {
        long id = jdbc.queryForObject("INSERT INTO reserva (solicitante_id, inicio, fim, estado, criada_em, prazo_aprovacao_em) "
                + "VALUES (?, ?::timestamptz, ?::timestamptz, ?, now(), ?::timestamptz) RETURNING id", Long.class,
                solicitante, inicio, fim, estado, inicio);
        for (long recurso : recursos) {
            jdbc.update("INSERT INTO reserva_recurso (reserva_id, recurso_id) VALUES (?, ?)", id, recurso);
        }
        return id;
    }

    private void manutencao(String inicio, String fim) {
        jdbc.update("INSERT INTO periodo_manutencao (recurso_id, inicio, fim, motivo, criado_por) "
                + "VALUES (?, ?::timestamptz, ?::timestamptz, 'Troca do projetor', ?)", sala, inicio, fim, solicitante);
    }

    private int contar(String tabela) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabela, Integer.class);
    }

    private static String sqlState(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql) {
                return sql.getSQLState();
            }
        }
        return null;
    }
}
