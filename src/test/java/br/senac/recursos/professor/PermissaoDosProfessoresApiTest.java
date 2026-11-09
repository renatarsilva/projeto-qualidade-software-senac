package br.senac.recursos.professor;

import static br.senac.recursos.professor.ProfessoresDeTeste.altera;
import static br.senac.recursos.professor.ProfessoresDeTeste.cadastra;
import static br.senac.recursos.professor.ProfessoresDeTeste.cadastraEDevolveId;
import static br.senac.recursos.professor.ProfessoresDeTeste.corpo;
import static br.senac.recursos.professor.ProfessoresDeTeste.corpoValido;
import static br.senac.recursos.professor.ProfessoresDeTeste.exclui;
import static br.senac.recursos.professor.ProfessoresDeTeste.limpaBanco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TesteDeApiComBanco;
import br.senac.recursos.seguranca.IdentidadesDeTeste;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Permissão por operação nos professores (spec T-17, CA-05, CA-08 e CA-12; casos TC-09, TC-13, TC-14 e TC-38).
 * Consulta: qualquer autenticado. Cadastro, alteração e exclusão: só o Administrador.
 */
@TesteDeApiComBanco
class PermissaoDosProfessoresApiTest {

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

    private String bearer(Perfil perfil) {
        return IdentidadesDeTeste.bearerDe(tokens, perfil);
    }

    @ParameterizedTest(name = "{0} consulta por id e a lista e recebe 200 com competências")
    @EnumSource(Perfil.class)
    void tc09_osTresPerfisConsultamPorIdEListaComCompetencias(Perfil perfil) throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpoValido("ana-souza-12345"));

        mvc.perform(get("/api/professores/" + id).header(HttpHeaders.AUTHORIZATION, bearer(perfil)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competencias").value(containsInAnyOrder("Banco de Dados", "Java")));
        mvc.perform(get("/api/professores").header(HttpHeaders.AUTHORIZATION, bearer(perfil)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0} não cadastra, altera nem exclui: 403")
    @EnumSource(value = Perfil.class, names = {"SOLICITANTE", "RESPONSAVEL"})
    void tc13_solicitanteEResponsavelRecebem403ENadaEGravado(Perfil perfil) throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpoValido("ana-souza-12345"));

        cadastra(mvc, tokens, perfil, corpoValido("bia-lima-22222")).andExpect(status().isForbidden());
        altera(mvc, tokens, perfil, id, corpo("ana-souza-12345", "true", "[]")).andExpect(status().isForbidden());
        exclui(mvc, tokens, perfil, id).andExpect(status().isForbidden());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM professor", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM competencia", Long.class)).isEqualTo(2);
    }

    @Test
    void tc14_anonimoRecebe401EmTodasAsOperacoes() throws Exception {
        String json = corpoValido("ana-souza-12345");
        mvc.perform(post("/api/professores").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/professores/1").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/professores/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/professores/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void tc38_solicitanteComNomeInvalidoRecebe403PoisOPerfilVemAntesDaValidacao() throws Exception {
        cadastra(mvc, tokens, Perfil.SOLICITANTE, corpo("Renata Silva", null, null))
                .andExpect(status().isForbidden());
    }
}
