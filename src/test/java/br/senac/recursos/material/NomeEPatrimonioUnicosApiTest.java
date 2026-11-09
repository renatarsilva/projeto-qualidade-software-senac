package br.senac.recursos.material;

import static br.senac.recursos.material.MateriaisDeTeste.altera;
import static br.senac.recursos.material.MateriaisDeTeste.cadastra;
import static br.senac.recursos.material.MateriaisDeTeste.cadastraEDevolveId;
import static br.senac.recursos.material.MateriaisDeTeste.corpo;
import static br.senac.recursos.material.MateriaisDeTeste.limpaBanco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TesteDeApiComBanco;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Nome e patrimônio únicos (spec T-18, CA-03 e CA-04; casos TC-06, TC-09, TC-20, TC-21, TC-24, TC-31 e TC-32).
 * Quando o serviço pega a repetição, a mensagem diz qual campo repetiu. Quando só o banco pega (corrida), é a genérica.
 */
@TesteDeApiComBanco
class NomeEPatrimonioUnicosApiTest {

    private static final String SO_NOME = "Já existe um material com esse nome";
    private static final String SO_PATRIMONIO = "Já existe um material com esse patrimônio";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private MaterialRepository materiais;

    @BeforeEach
    void limpar() {
        limpaBanco(jdbc);
    }

    @Test
    void tc06_patrimonioRepetidoSemDiferenciarMaiusculasDa409CitandoOPatrimonio() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("Projetor A", "Projetor", "pat-1", null))
                .andExpect(status().isCreated());

        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("Projetor B", "Projetor", "PAT-1", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(SO_PATRIMONIO));
    }

    @Test
    void tc20_mesmoNomeComOutroPatrimonioDa409CitandoONome() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("Projetor Epson", "Projetor", "PAT-1", null))
                .andExpect(status().isCreated());

        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("Projetor Epson", "Projetor", "PAT-2", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(SO_NOME));
    }

    @Test
    void tc24_nomeIgualComEspacosEMaiusculasDiferentesDa409CitandoONome() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("Projetor Epson", "Projetor", "PAT-1", null))
                .andExpect(status().isCreated());

        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("projetor  epson", "Projetor", "PAT-2", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(SO_NOME));
    }

    @Test
    void tc31_nomeEPatrimonioJaExistentesDa409CitandoOsDois() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("Projetor Epson", "Projetor", "PAT-1", null))
                .andExpect(status().isCreated());

        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("Projetor Epson", "Projetor", "PAT-1", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um material com esse nome e esse patrimônio"));
    }

    @Test
    void tc09_alterarOPatrimonioParaODeOutroMaterialDa409() throws Exception {
        cadastraEDevolveId(mvc, tokens, corpo("Projetor A", "Projetor", "PAT-1", "false"));
        long segundo = cadastraEDevolveId(mvc, tokens, corpo("Projetor B", "Projetor", "PAT-2", "false"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, segundo, corpo("Projetor B", "Projetor", "PAT-1", "false"))
                .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT patrimonio FROM material WHERE recurso_id = ?", String.class, segundo))
                .isEqualTo("PAT-2");
    }

    @Test
    void tc21_alterarONomeParaODeOutroMaterialDa409() throws Exception {
        cadastraEDevolveId(mvc, tokens, corpo("Projetor A", "Projetor", "PAT-1", "false"));
        long segundo = cadastraEDevolveId(mvc, tokens, corpo("Projetor B", "Projetor", "PAT-2", "false"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, segundo, corpo("Projetor A", "Projetor", "PAT-2", "false"))
                .andExpect(status().isConflict());
    }

    @Test
    void tc32_corridaSimuladaOBancoBarraComAMensagemFixaENuncaDa500() throws Exception {
        // O material entra direto no banco e a verificação do serviço é burlada: só o banco decide.
        long recurso = jdbc.queryForObject(
                "INSERT INTO recurso (tipo, nome, restrito) VALUES ('MATERIAL', 'Projetor Epson', false) RETURNING id",
                Long.class);
        jdbc.update("INSERT INTO material (recurso_id, tipo_material, patrimonio) VALUES (?, 'Projetor', 'PAT-1')",
                recurso);
        doReturn(false).when(materiais).existsByNomeIgnoreCase(anyString());
        doReturn(false).when(materiais).existsByPatrimonioIgnoreCase(anyString());

        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("Projetor Epson", "Projetor", "PAT-1", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta operação conflita com outro registro"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM material", Long.class)).isEqualTo(1);
    }
}
