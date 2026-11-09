package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static br.senac.recursos.reserva.HorariosDeTeste.AGORA;
import static br.senac.recursos.reserva.HorariosDeTeste.segunda;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.TokenService;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Contrato do PUT /api/reservas/{id} (spec T-20, seção 4; TC-35, TC-41, TC-50, TC-60, TC-61, TC-63 a TC-67). */
@TesteDeReservaApi
class AlteracaoDeReservaApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RelogioDeTeste relogio;

    private AmbienteDeReserva ambiente;
    private RecursoPedido sala101;
    private RecursoPedido sala102;
    private long dono;

    @BeforeEach
    void preparar() throws Exception {
        ambiente = new AmbienteDeReserva(mvc, tokens, jdbc);
        ambiente.limpa();
        relogio.volta();
        sala101 = ambiente.sala("sala-101");
        sala102 = ambiente.sala("sala-102");
        dono = ambiente.solicitante();
    }

    @AfterEach
    void limpar() {
        ambiente.limpa();
        relogio.volta();
    }

    private long reservaDoDono(EstadoDaReserva estado, RecursoPedido... recursos) {
        return ambiente.reservaGravada(dono, estado, terca(10, 0), terca(11, 0), AGORA, recursos);
    }

    private void assertNaoMudou(long id) {
        assertThat(ambiente.inicioNoBanco(id)).isEqualTo(terca(10, 0));
        assertThat(ambiente.fimNoBanco(id)).isEqualTo(terca(11, 0));
    }

    @Test
    @DisplayName("TC-35 (RN-14-T11): alterar uma APROVADA devolve 200, SOLICITADA, o aviso e o prazo recalculado")
    void alterarAprovadaDevolveAviso() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.APROVADA, sala102);

        String resposta = ambiente.altera(ambiente.bearerDoSolicitante(), id, corpo(terca(14, 0), terca(15, 0), sala102))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SOLICITADA"))
                .andExpect(jsonPath("$.aviso").value("A reserva precisa ser aprovada de novo"))
                .andReturn().getResponse().getContentAsString();

        // 30 h de antecedência: o prazo é o novo início menos 24 h (segunda, 14:00).
        assertThat(Instant.parse(JsonPath.read(resposta, "$.prazoAprovacaoEm"))).isEqualTo(segunda(14, 0));
    }

    @Test
    @DisplayName("TC-41: alterar uma reserva CONCLUIDA devolve 409 e ela não muda")
    void alterarConcluidaDevolve409() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.CONCLUIDA, sala102);

        ambiente.altera(ambiente.bearerDoSolicitante(), id, corpo(terca(14, 0), terca(15, 0), sala102))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta reserva não pode mais ser alterada"));
        assertNaoMudou(id);
    }

    @Test
    @DisplayName("TC-50: o PUT com o Clock em 08:30 preserva o criada_em das 08:00, na resposta e no banco")
    void alteracaoPreservaCriadaEm() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA, sala102);
        relogio.defineAgora(segunda(8, 30));

        String resposta = ambiente.altera(ambiente.bearerDoSolicitante(), id, corpo(terca(14, 0), terca(15, 0), sala102))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(Instant.parse(JsonPath.read(resposta, "$.criadaEm"))).isEqualTo(AGORA);
        assertThat(ambiente.criadaEmNoBanco(id)).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("TC-60: trocar a sala 101 pela 102 livre é aceito e libera a 101 para outro usuário")
    void trocaDeSalaLiberaAnterior() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA, sala101);

        ambiente.altera(ambiente.bearerDoSolicitante(), id, corpo(terca(10, 0), terca(11, 0), sala102))
                .andExpect(status().isOk());

        ambiente.cria(ambiente.bearerDoOutro(), corpo(terca(10, 0), terca(11, 0), sala101))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-61: sala nova livre e projetor novo ocupado recusam por inteiro; a reserva fica como estava")
    void alteracaoDeRecursosEAtomica() throws Exception {
        RecursoPedido projetor1 = ambiente.material("Projetor 1", "PAT-1");
        RecursoPedido projetor2 = ambiente.material("Projetor 2", "PAT-2");
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA, sala101, projetor1);
        ambiente.reservaGravada(ambiente.outroSolicitante(), EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0),
                AGORA, projetor2);

        ambiente.altera(ambiente.bearerDoSolicitante(), id, corpo(terca(10, 0), terca(11, 0), sala102, projetor2))
                .andExpect(status().isConflict());

        assertThat(ambiente.recursosNoBanco(id)).containsExactlyInAnyOrder(sala101.id(), projetor1.id());
        assertNaoMudou(id);
    }

    @Test
    @DisplayName("TC-63 (CA-18): PUT de reserva que não existe devolve 404")
    void reservaInexistenteDevolve404() throws Exception {
        ambiente.altera(ambiente.bearerDoSolicitante(), 999999, corpo(terca(10, 0), terca(11, 0), sala102))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Recurso não encontrado"));
    }

    @ParameterizedTest(name = "TC-64: PUT por {0} devolve 403")
    @EnumSource(value = Perfil.class, names = {"RESPONSAVEL", "ADMINISTRADOR"})
    @DisplayName("TC-64 (CA-18): quem não é SOLICITANTE recebe 403 e a reserva não muda")
    void perfilSemPermissaoDevolve403(Perfil perfil) throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA, sala102);

        ambiente.altera(ambiente.bearerDe(perfil), id, corpo(terca(14, 0), terca(15, 0), sala102))
                .andExpect(status().isForbidden());
        assertNaoMudou(id);
    }

    @Test
    @DisplayName("TC-65 (CA-18): SOLICITANTE que não é o dono recebe 403 e a reserva não muda")
    void naoDonoDevolve403() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA, sala102);

        ambiente.altera(ambiente.bearerDoOutro(), id, corpo(terca(14, 0), terca(15, 0), sala102))
                .andExpect(status().isForbidden());
        assertNaoMudou(id);
    }

    @Test
    @DisplayName("TC-66 (CA-18): PUT sem autenticação devolve 401 e a reserva não muda")
    void semAutenticacaoDevolve401() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA, sala102);

        ambiente.altera(null, id, corpo(terca(14, 0), terca(15, 0), sala102))
                .andExpect(status().isUnauthorized());
        assertNaoMudou(id);
    }

    @Test
    @DisplayName("TC-67 (CA-18): quem não é o dono recebe 403 antes do 400 do corpo inválido")
    void donoVemAntesDoCorpoInvalido() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA, sala102);

        ambiente.altera(ambiente.bearerDoOutro(), id, corpo(terca(11, 0), terca(10, 0), sala102))
                .andExpect(status().isForbidden());
        assertNaoMudou(id);
    }

    @Test
    @DisplayName("TC-67 (CA-18): reserva inexistente recebe 404 antes do 400 do corpo inválido")
    void existenciaVemAntesDoCorpoInvalido() throws Exception {
        ambiente.altera(ambiente.bearerDoSolicitante(), 999999, corpo(terca(11, 0), terca(10, 0), sala102))
                .andExpect(status().isNotFound());
    }
}
