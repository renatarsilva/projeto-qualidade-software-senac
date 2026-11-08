package br.senac.recursos.professor;

import static br.senac.recursos.professor.ProfessoresDeTeste.cadastra;
import static br.senac.recursos.professor.ProfessoresDeTeste.corpo;
import static br.senac.recursos.professor.ProfessoresDeTeste.corpoValido;
import static br.senac.recursos.professor.ProfessoresDeTeste.limpaBanco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TesteDeApiComBanco;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cadastro de professor pela API (spec T-17, CA-01, CA-02 e CA-10;
 * casos TC-01, TC-02, TC-22, TC-28, TC-34, TC-41 e TC-42).
 */
@TesteDeApiComBanco
class CadastroDeProfessorApiTest {

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

    @Test
    void tc01_cadastroComNomeEDuasCompetenciasDa201ComAsDuas() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpoValido("ana-souza-12345"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("ana-souza-12345"))
                .andExpect(jsonPath("$.competencias").value(containsInAnyOrder("Banco de Dados", "Java")));
    }

    @Test
    void tc02_cadastroComListaDeCompetenciasVaziaDa201() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("ana-souza-12345", "false", "[]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.competencias").value(empty()));
    }

    @Test
    void tc34_semRestritoESemCompetenciasValeFalsoEListaVazia() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("ana-souza-12345", null, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.restrito").value(false))
                .andExpect(jsonPath("$.competencias").value(empty()));
    }

    @Test
    void tc28_nomeComEspacoDa400ApontandoONomeENadaEGravado() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("Renata Silva", "false", "[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='nome')].motivo")
                        .value(hasItem("deve seguir o padrão primeironome-ultimonome-matricula")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM recurso", Long.class)).isZero();
    }

    @Test
    void tc41_restritoQueNaoEBooleanoDa400SemConversao() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("ana-souza-12345", "\"talvez\"", "[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='restrito')].motivo").value(hasItem("valor inválido")));
    }

    @Test
    void tc42_competenciasQueNaoEListaDa400() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("ana-souza-12345", "false", "\"Java\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='competencias')].motivo").value(hasItem("valor inválido")));
    }

    @Test
    void tc22_homonimosComMatriculasDiferentesConvivem() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpoValido("ana-souza-11111")).andExpect(status().isCreated());
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpoValido("ana-souza-22222")).andExpect(status().isCreated());
    }
}
