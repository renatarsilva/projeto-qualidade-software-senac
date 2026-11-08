package br.senac.recursos.material;

import static br.senac.recursos.material.MateriaisDeTeste.altera;
import static br.senac.recursos.material.MateriaisDeTeste.cadastraEDevolveId;
import static br.senac.recursos.material.MateriaisDeTeste.corpo;
import static br.senac.recursos.material.MateriaisDeTeste.limpaBanco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TesteDeApiComBanco;
import br.senac.recursos.seguranca.IdentidadesDeTeste;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Alteração de material pela API (spec T-18, CA-04 e CA-06;
 * casos TC-07, TC-08, TC-11, TC-19, TC-29, TC-30 e TC-34). O restrito é obrigatório no PUT (P-04).
 */
@TesteDeApiComBanco
class AlteracaoDeMaterialApiTest {

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
    void tc07_alterarONomeEOTipoDa200ComOsDadosNovos() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpo("Projetor Epson", "Projetor", "PAT-1", "false"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, id, corpo("Notebook Dell", "Notebook", "PAT-1", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Notebook Dell"))
                .andExpect(jsonPath("$.tipoMaterial").value("Notebook"));
    }

    @Test
    void tc08_alterarMantendoOProprioPatrimonioDa200() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpo("Projetor Epson", "Projetor", "PAT-1", "false"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, id, corpo("Projetor Epson 2", "Projetor", "PAT-1", "false"))
                .andExpect(status().isOk());
    }

    @Test
    void tc11_consultarOuAlterarMaterialInexistenteDa404() throws Exception {
        mvc.perform(get("/api/materiais/999999")
                        .header(HttpHeaders.AUTHORIZATION, IdentidadesDeTeste.bearerDe(tokens, Perfil.ADMINISTRADOR)))
                .andExpect(status().isNotFound());
        altera(mvc, tokens, Perfil.ADMINISTRADOR, 999999, corpo("Projetor Epson", "Projetor", "PAT-1", "false"))
                .andExpect(status().isNotFound());
    }

    @Test
    void tc19_putSemRestritoDa400ENaoTiraAExigenciaDeAprovacao() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpo("Projetor Epson", "Projetor", "PAT-1", "true"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, id, corpo("Projetor Epson", "Projetor", "PAT-1", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("restrito")));

        assertThat(jdbc.queryForObject("SELECT restrito FROM recurso WHERE id = ?", Boolean.class, id)).isTrue();
    }

    @Test
    void tc29_alterarOProprioNomeSoMudandoMaiusculasNaoConflitaComElePropria() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpo("Projetor Epson", "Projetor", "PAT-1", "false"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, id, corpo("projetor epson", "Projetor", "PAT-1", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("projetor epson"));
    }

    @Test
    void tc30_alterarOProprioPatrimonioComEspacosEMaiusculasDa200EGravaLimpo() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpo("Projetor Epson", "Projetor", "PAT-1", "false"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, id, corpo("Projetor Epson", "Projetor", " pat-1 ", "false"))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT patrimonio FROM material WHERE recurso_id = ?", String.class, id))
                .isEqualTo("pat-1");
    }

    @Test
    void tc34_idInexistenteComCorpoInvalidoDa404ENaoDa400() throws Exception {
        altera(mvc, tokens, Perfil.ADMINISTRADOR, 999999, corpo(null, null, null, null))
                .andExpect(status().isNotFound());
    }
}
