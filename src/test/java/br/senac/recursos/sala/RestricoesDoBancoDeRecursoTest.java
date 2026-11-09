package br.senac.recursos.sala;

import static br.senac.recursos.sala.SalasDeTeste.cadastra;
import static br.senac.recursos.sala.SalasDeTeste.corpoValido;
import static br.senac.recursos.sala.SalasDeTeste.limpaBanco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TesteDeApiComBanco;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O banco é a última linha de defesa (spec T-16, CA-09 e CA-12; casos TC-18, TC-19, TC-25, TC-26, TC-39 e TC-40).
 * Os INSERTs vão direto no banco, sem passar pela aplicação.
 */
@TesteDeApiComBanco
class RestricoesDoBancoDeRecursoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpar() {
        limpaBanco(jdbc);
    }

    private long insereRecurso(String tipo, String nome) {
        return jdbc.queryForObject(
                "INSERT INTO recurso (tipo, nome, restrito) VALUES (?, ?, false) RETURNING id", Long.class, tipo, nome);
    }

    @Test
    void tc18_gravarUmaSalaCriaUmaLinhaEmRecursoEUmaEmSala() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpoValido("sala-170")).andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM recurso WHERE tipo = 'SALA'", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sala", Long.class)).isEqualTo(1);
    }

    @Test
    void tc19_capacidadeZeroNoBancoERecusadaPeloCheck() {
        long id = insereRecurso("SALA", "sala-171");

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO sala (recurso_id, capacidade, localizacao, tipo_sala) VALUES (?, 0, 'Bloco B', 'Lab')", id))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tc25_nomeForaDoPadraoParaSalaERecusadoPeloCheck() {
        assertThatThrownBy(() -> insereRecurso("SALA", "Sala-01"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tc26_mesmoTipoEMesmoNomeSaoRecusadosPeloIndiceUnico() {
        insereRecurso("SALA", "sala-172");

        assertThatThrownBy(() -> insereRecurso("SALA", "sala-172"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tc39_oCheckDoPadraoDeSalaSoValeParaOTipoSala() {
        assertThatCode(() -> insereRecurso("MATERIAL", "Projetor Epson")).doesNotThrowAnyException();
        assertThatCode(() -> insereRecurso("PROFESSOR", "ana-souza-12345")).doesNotThrowAnyException();
    }

    @Test
    void tc40_oNomeDoRecursoCabeEmCemCaracteres() {
        assertThatCode(() -> insereRecurso("MATERIAL", "m".repeat(100))).doesNotThrowAnyException();
        assertThatThrownBy(() -> insereRecurso("MATERIAL", "m".repeat(101)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
