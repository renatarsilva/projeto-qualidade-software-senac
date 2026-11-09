package br.senac.recursos.material;

import static br.senac.recursos.material.MateriaisDeTeste.altera;
import static br.senac.recursos.material.MateriaisDeTeste.cadastra;
import static br.senac.recursos.material.MateriaisDeTeste.cadastraEDevolveId;
import static br.senac.recursos.material.MateriaisDeTeste.corpo;
import static br.senac.recursos.material.MateriaisDeTeste.corpoValido;
import static br.senac.recursos.material.MateriaisDeTeste.exclui;
import static br.senac.recursos.material.MateriaisDeTeste.limpaBanco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Permissão por operação nos materiais (spec T-18, CA-05 e CA-08; casos TC-10, TC-14, TC-15 e TC-33).
 * Consulta: qualquer autenticado. Cadastro, alteração e exclusão: só o Administrador.
 */
@TesteDeApiComBanco
class PermissaoDosMateriaisApiTest {

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

    @ParameterizedTest(name = "{0} consulta por id e a lista e recebe 200")
    @EnumSource(Perfil.class)
    void tc10_osTresPerfisConsultamPorIdEALista(Perfil perfil) throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpoValido("Projetor Epson", "PAT-1"));

        mvc.perform(get("/api/materiais/" + id).header(HttpHeaders.AUTHORIZATION, bearer(perfil)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/materiais").header(HttpHeaders.AUTHORIZATION, bearer(perfil)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0} não cadastra, altera nem exclui: 403")
    @EnumSource(value = Perfil.class, names = {"SOLICITANTE", "RESPONSAVEL"})
    void tc14_solicitanteEResponsavelRecebem403ENadaEGravado(Perfil perfil) throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpoValido("Projetor Epson", "PAT-1"));

        cadastra(mvc, tokens, perfil, corpoValido("Notebook Dell", "PAT-2")).andExpect(status().isForbidden());
        altera(mvc, tokens, perfil, id, corpo("Projetor Epson", "Projetor", "PAT-1", "true"))
                .andExpect(status().isForbidden());
        exclui(mvc, tokens, perfil, id).andExpect(status().isForbidden());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM material", Long.class)).isEqualTo(1);
    }

    @Test
    void tc15_anonimoRecebe401EmTodasAsOperacoes() throws Exception {
        String json = corpoValido("Projetor Epson", "PAT-1");
        mvc.perform(post("/api/materiais").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/materiais/1").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/materiais/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/materiais/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void tc33_solicitanteComPutInvalidoEmIdInexistenteRecebe403() throws Exception {
        altera(mvc, tokens, Perfil.SOLICITANTE, 999999, corpo(null, null, null, null))
                .andExpect(status().isForbidden());
    }
}
