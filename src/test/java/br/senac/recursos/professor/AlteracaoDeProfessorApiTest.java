package br.senac.recursos.professor;

import static br.senac.recursos.professor.ProfessoresDeTeste.altera;
import static br.senac.recursos.professor.ProfessoresDeTeste.cadastraEDevolveId;
import static br.senac.recursos.professor.ProfessoresDeTeste.corpo;
import static br.senac.recursos.professor.ProfessoresDeTeste.limpaBanco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TesteDeApiComBanco;
import br.senac.recursos.seguranca.IdentidadesDeTeste;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.TokenService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Alteração de professor pela API (spec T-17, CA-04 e CA-06; casos TC-08, TC-10, TC-29, TC-31, TC-32, TC-33 e TC-39).
 * O PUT substitui o professor por inteiro: restrito e competencias são obrigatórios (P-03).
 */
@TesteDeApiComBanco
class AlteracaoDeProfessorApiTest {

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

    private List<String> competenciasGravadas() {
        return jdbc.queryForList("SELECT descricao FROM competencia ORDER BY descricao", String.class);
    }

    @Test
    void tc08_alterarDeJavaESqlParaPythonDeixaSoPython() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpo("ana-souza-12345", "false", "[\"Java\",\"SQL\"]"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, id, corpo("ana-souza-12345", "false", "[\"Python\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competencias").value(contains("Python")));

        assertThat(competenciasGravadas()).containsExactly("Python");
    }

    @Test
    void tc29_alterarMantendoOProprioNomeETrocandoAsCompetenciasDa200() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpo("ana-souza-12345", "false", "[\"Java\"]"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, id, corpo("ana-souza-12345", "true", "[\"Redes\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restrito").value(true));
    }

    @Test
    void tc31_putSemRestritoDa400ENaoMudaOProfessor() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpo("ana-souza-12345", "true", "[\"Java\"]"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, id, corpo("ana-souza-12345", null, "[\"Python\"]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("restrito")));

        assertThat(competenciasGravadas()).containsExactly("Java");
        assertThat(jdbc.queryForObject("SELECT restrito FROM recurso WHERE id = ?", Boolean.class, id)).isTrue();
    }

    @Test
    void tc32_putSemCompetenciasDa400ENaoMudaOProfessor() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpo("ana-souza-12345", "false", "[\"Java\"]"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, id, corpo("ana-souza-12345", "false", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("competencias")));

        assertThat(competenciasGravadas()).containsExactly("Java");
    }

    @Test
    void tc33_putComListaVaziaRemoveTodasAsCompetencias() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpo("ana-souza-12345", "false", "[\"Java\",\"SQL\"]"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, id, corpo("ana-souza-12345", "false", "[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competencias").value(empty()));

        assertThat(competenciasGravadas()).isEmpty();
    }

    @Test
    void tc10_consultarOuAlterarProfessorInexistenteDa404() throws Exception {
        mvc.perform(get("/api/professores/999999")
                        .header(HttpHeaders.AUTHORIZATION, IdentidadesDeTeste.bearerDe(tokens, Perfil.ADMINISTRADOR)))
                .andExpect(status().isNotFound());
        altera(mvc, tokens, Perfil.ADMINISTRADOR, 999999, corpo("ana-souza-12345", "false", "[]"))
                .andExpect(status().isNotFound());
    }

    @Test
    void tc39_idInexistenteComCorpoInvalidoDa404ENaoDa400() throws Exception {
        altera(mvc, tokens, Perfil.ADMINISTRADOR, 999999, corpo("Renata Silva", null, null))
                .andExpect(status().isNotFound());
    }
}
