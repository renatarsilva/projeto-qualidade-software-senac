package br.senac.recursos.sala;

import static br.senac.recursos.sala.SalasDeTeste.altera;
import static br.senac.recursos.sala.SalasDeTeste.cadastra;
import static br.senac.recursos.sala.SalasDeTeste.cadastraEDevolveId;
import static br.senac.recursos.sala.SalasDeTeste.corpo;
import static br.senac.recursos.sala.SalasDeTeste.corpoValido;
import static br.senac.recursos.sala.SalasDeTeste.limpaBanco;
import static org.assertj.core.api.Assertions.assertThat;
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
 * Permissão por operação nas salas (spec T-16, CA-06 e CA-08; casos TC-14, TC-16, TC-17 e TC-41).
 * Consulta: qualquer autenticado. Cadastro e alteração: só o Administrador.
 */
@TesteDeApiComBanco
class PermissaoDasSalasApiTest {

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
    void tc14_osTresPerfisConsultamPorIdEALista(Perfil perfil) throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpoValido("sala-160"));

        mvc.perform(get("/api/salas/" + id).header(HttpHeaders.AUTHORIZATION, bearer(perfil)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/salas").header(HttpHeaders.AUTHORIZATION, bearer(perfil)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0} não cadastra nem altera: 403")
    @EnumSource(value = Perfil.class, names = {"SOLICITANTE", "RESPONSAVEL"})
    void tc16_solicitanteEResponsavelRecebem403NoPostENoPutENadaEGravado(Perfil perfil) throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpoValido("sala-161"));

        cadastra(mvc, tokens, perfil, corpoValido("sala-162")).andExpect(status().isForbidden());
        altera(mvc, tokens, perfil, id, corpo("sala-161", "Laboratório", "99", "Bloco B", "false"))
                .andExpect(status().isForbidden());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM sala", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT capacidade FROM sala WHERE recurso_id = ?", Integer.class, id))
                .isEqualTo(30);
    }

    @Test
    void tc17_anonimoRecebe401NoPostNoPutENoGet() throws Exception {
        mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON).content(corpoValido("sala-163")))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/salas/1").contentType(MediaType.APPLICATION_JSON).content(corpoValido("sala-163")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/salas/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void tc41_aOrdemDosErrosEPerfilAntesDeExistenciaAntesDeValidacao() throws Exception {
        // Solicitante com corpo inválido: 403 (o perfil vem antes da validação).
        cadastra(mvc, tokens, Perfil.SOLICITANTE, corpo("Sala-01", null, "0", null, null))
                .andExpect(status().isForbidden());

        // Administrador, id inexistente e corpo inválido: 404 (a existência vem antes da validação).
        altera(mvc, tokens, Perfil.ADMINISTRADOR, 999999, corpo("Sala-01", null, "0", null, null))
                .andExpect(status().isNotFound());
    }
}
