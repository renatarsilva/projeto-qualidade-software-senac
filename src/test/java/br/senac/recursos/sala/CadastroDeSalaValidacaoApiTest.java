package br.senac.recursos.sala;

import static br.senac.recursos.sala.SalasDeTeste.cadastra;
import static br.senac.recursos.sala.SalasDeTeste.corpo;
import static br.senac.recursos.sala.SalasDeTeste.limpaBanco;
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
 * Validação do cadastro de sala pela API (spec T-16, CA-03, CA-10 e CA-11;
 * casos TC-08, TC-29, TC-31, TC-32, TC-33, TC-35 e TC-37).
 */
@TesteDeApiComBanco
class CadastroDeSalaValidacaoApiTest {

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

    private long totalDeRecursos() {
        return jdbc.queryForObject("SELECT count(*) FROM recurso", Long.class);
    }

    @Test
    void tc08_pedidoSemUmCampoObrigatorioApontaSoEsseCampo() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("sala-130", null, "30", "Bloco B", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(containsInAnyOrder("tipoSala")));
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("sala-130", "Laboratório", null, "Bloco B", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(containsInAnyOrder("capacidade")));
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("sala-130", "Laboratório", "30", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(containsInAnyOrder("localizacao")));
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("sala-130", null, null, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo")
                        .value(containsInAnyOrder("tipoSala", "capacidade", "localizacao")));
        assertThat(totalDeRecursos()).isZero();
    }

    @Test
    void tc29_tipoELocalizacaoSaoGravadosSemOsEspacosExtras() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("sala-131", "  Laboratório ", "30", "Bloco    B", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoSala").value("Laboratório"))
                .andExpect(jsonPath("$.localizacao").value("Bloco B"));
        String gravado = jdbc.queryForObject("SELECT tipo_sala FROM sala", String.class);
        assertThat(gravado).isEqualTo("Laboratório");
    }

    @Test
    void tc31_nomeAusenteOuSoComEspacosDa400ApontandoONome() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo(null, "Laboratório", "30", "Bloco B", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("nome")));
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("   ", "Laboratório", "30", "Bloco B", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("nome")));
    }

    @Test
    void tc32_nomeForaDoPadraoDa400ComOMotivoENadaEGravado() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("Sala-01", "Laboratório", "30", "Bloco B", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='nome')].motivo").value(hasItem("deve seguir o padrão sala-NN")));
        assertThat(totalDeRecursos()).isZero();
    }

    @Test
    void tc33_tipoOuLocalizacaoComCentoEUmCaracteresSaoRecusados() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("sala-132", "t".repeat(101), "30", "Bloco B", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("tipoSala")));
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("sala-132", "Laboratório", "30", "l".repeat(101), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("localizacao")));
    }

    @Test
    void tc35_salaZeroUmEZeroZeroUmConvivem() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("sala-01", "Laboratório", "30", "Bloco B", null))
                .andExpect(status().isCreated());
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("sala-001", "Laboratório", "30", "Bloco B", null))
                .andExpect(status().isCreated());
    }

    @Test
    void tc37_tipoOuLocalizacaoSoComEspacosSaoRecusados() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("sala-133", "   ", "30", "Bloco B", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("tipoSala")));
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("sala-133", "Laboratório", "30", "   ", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("localizacao")));
    }
}
