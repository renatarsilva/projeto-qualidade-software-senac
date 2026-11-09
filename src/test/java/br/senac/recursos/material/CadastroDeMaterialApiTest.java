package br.senac.recursos.material;

import static br.senac.recursos.material.MateriaisDeTeste.cadastra;
import static br.senac.recursos.material.MateriaisDeTeste.corpo;
import static br.senac.recursos.material.MateriaisDeTeste.limpaBanco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
 * Cadastro de material pela API (spec T-18, CA-01 e CA-02;
 * casos TC-01, TC-02, TC-03, TC-04, TC-05, TC-18, TC-23, TC-25 e TC-26).
 */
@TesteDeApiComBanco
class CadastroDeMaterialApiTest {

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

    private org.springframework.test.web.servlet.ResultActions cadastraComoAdmin(String json) throws Exception {
        return cadastra(mvc, tokens, Perfil.ADMINISTRADOR, json);
    }

    @Test
    void tc01_cadastroComNomeTipoEPatrimonioDa201ComRestritoFalso() throws Exception {
        cadastraComoAdmin(corpo("Projetor Epson", "Projetor", "PAT-000123", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.restrito").value(false));
    }

    @Test
    void tc02_nomeTipoOuPatrimonioAusentesOuSoComEspacosApontamCadaCampo() throws Exception {
        cadastraComoAdmin(corpo(null, null, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo")
                        .value(containsInAnyOrder("nome", "tipoMaterial", "patrimonio")));
        cadastraComoAdmin(corpo("   ", "   ", "   ", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo")
                        .value(containsInAnyOrder("nome", "tipoMaterial", "patrimonio")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM recurso", Long.class)).isZero();
    }

    @Test
    void tc03_patrimonioComTrintaCaracteresEAceitoEComTrintaEUmERecusado() throws Exception {
        cadastraComoAdmin(corpo("Projetor A", "Projetor", "p".repeat(30), null)).andExpect(status().isCreated());
        cadastraComoAdmin(corpo("Projetor B", "Projetor", "q".repeat(31), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("patrimonio")));
    }

    @Test
    void tc04_nomeComCemCaracteresEAceitoEComCentoEUmERecusado() throws Exception {
        cadastraComoAdmin(corpo("n".repeat(100), "Projetor", "PAT-1", null)).andExpect(status().isCreated());
        cadastraComoAdmin(corpo("m".repeat(101), "Projetor", "PAT-2", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("nome")));
    }

    @Test
    void tc05_patrimonioComEspacosNasPontasEGravadoSemEles() throws Exception {
        cadastraComoAdmin(corpo("Projetor Epson", "Projetor", "  PAT-5  ", null)).andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT patrimonio FROM material", String.class)).isEqualTo("PAT-5");
    }

    @Test
    void tc18_tipoComCemCaracteresEAceitoEComCentoEUmERecusado() throws Exception {
        cadastraComoAdmin(corpo("Projetor A", "t".repeat(100), "PAT-1", null)).andExpect(status().isCreated());
        cadastraComoAdmin(corpo("Projetor B", "u".repeat(101), "PAT-2", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("tipoMaterial")));
    }

    @Test
    void tc23_nomeComEspacosExtrasEGravadoLimpo() throws Exception {
        cadastraComoAdmin(corpo("  Projetor   Epson ", "Projetor", "PAT-1", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Projetor Epson"));

        assertThat(jdbc.queryForObject("SELECT nome FROM recurso", String.class)).isEqualTo("Projetor Epson");
    }

    @Test
    void tc25_oTamanhoDoNomeContaDepoisDaLimpeza() throws Exception {
        cadastraComoAdmin(corpo("  " + "n".repeat(100) + "  ", "Projetor", "PAT-1", null))
                .andExpect(status().isCreated());
        cadastraComoAdmin(corpo("  " + "m".repeat(101) + "  ", "Projetor", "PAT-2", null))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc26_patrimonioComEspacosRepetidosNoMeioEGravadoComUmSo() throws Exception {
        cadastraComoAdmin(corpo("Projetor Epson", "Projetor", "PAT   123", null)).andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT patrimonio FROM material", String.class)).isEqualTo("PAT 123");
    }
}
