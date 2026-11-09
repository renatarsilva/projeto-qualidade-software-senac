package br.senac.recursos.professor;

import static br.senac.recursos.professor.ProfessoresDeTeste.cadastra;
import static br.senac.recursos.professor.ProfessoresDeTeste.corpoValido;
import static br.senac.recursos.professor.ProfessoresDeTeste.limpaBanco;
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
 * O banco é a última linha de defesa (spec T-17, CA-09 e CA-11; casos TC-15, TC-18, TC-21, TC-24 e TC-25).
 * Os INSERTs vão direto no banco, sem passar pela aplicação.
 */
@TesteDeApiComBanco
class RestricoesDoBancoDeProfessorTest {

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

    private long insereProfessor(String nome, String matricula) {
        long recurso = insereRecurso("PROFESSOR", nome);
        jdbc.update("INSERT INTO professor (recurso_id, matricula) VALUES (?, ?)", recurso, matricula);
        return recurso;
    }

    @Test
    void tc15_gravarProfessorComDuasCompetenciasCriaLinhasEmRecursoProfessorECompetencia() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpoValido("ana-souza-12345")).andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM recurso WHERE tipo = 'PROFESSOR'", Long.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT matricula FROM professor", String.class)).isEqualTo("12345");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM competencia", Long.class)).isEqualTo(2);
    }

    @Test
    void tc18_mesmoTipoEMesmoNomeSaoRecusadosPeloIndiceUnico() {
        insereRecurso("PROFESSOR", "ana-souza-12345");

        assertThatThrownBy(() -> insereRecurso("PROFESSOR", "ana-souza-12345"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tc21_nomeForaDoPadraoParaProfessorERecusadoPeloCheck() {
        assertThatThrownBy(() -> insereRecurso("PROFESSOR", "Ana Souza"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tc24_matriculaRepetidaNoBancoERecusadaPeloUnique() {
        insereProfessor("ana-souza-12345", "12345");

        assertThatThrownBy(() -> insereProfessor("bia-lima-12345", "12345"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tc25_oCheckDoProfessorNaoSeAplicaAOutroTipo() {
        assertThatCode(() -> insereRecurso("SALA", "sala-01")).doesNotThrowAnyException();
    }
}
