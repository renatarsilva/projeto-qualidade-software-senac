package br.senac.recursos.material;

import static br.senac.recursos.material.MateriaisDeTeste.cadastra;
import static br.senac.recursos.material.MateriaisDeTeste.cadastraEDevolveId;
import static br.senac.recursos.material.MateriaisDeTeste.corpo;
import static br.senac.recursos.material.MateriaisDeTeste.exclui;
import static br.senac.recursos.material.MateriaisDeTeste.limpaBanco;
import static org.assertj.core.api.Assertions.assertThat;
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
 * Exclusão de material pela API (spec T-18, CA-06 e CA-07; casos TC-11 e TC-12).
 * A recusa de material com reserva, retirada ou manutenção é testada na T-61.
 */
@TesteDeApiComBanco
class ExclusaoDeMaterialApiTest {

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

    private long linhas(String tabela) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabela, Long.class);
    }

    @Test
    void tc12_excluirMaterialSemVinculoDa204ELiberaONomeEOPatrimonio() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpo("Projetor Epson", "Projetor", "PAT-1", "false"));

        exclui(mvc, tokens, Perfil.ADMINISTRADOR, id).andExpect(status().isNoContent());

        assertThat(linhas("material")).isZero();
        assertThat(linhas("recurso")).isZero();
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("Projetor Epson", "Projetor", "PAT-1", "false"))
                .andExpect(status().isCreated());
    }

    @Test
    void tc11_excluirMaterialInexistenteDa404() throws Exception {
        exclui(mvc, tokens, Perfil.ADMINISTRADOR, 999999).andExpect(status().isNotFound());
    }
}
