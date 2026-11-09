package br.senac.recursos.sala;

import static br.senac.recursos.sala.SalasDeTeste.altera;
import static br.senac.recursos.sala.SalasDeTeste.cadastraEDevolveId;
import static br.senac.recursos.sala.SalasDeTeste.corpo;
import static br.senac.recursos.sala.SalasDeTeste.corpoValido;
import static br.senac.recursos.sala.SalasDeTeste.limpaBanco;
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
 * Alteração de sala pela API (spec T-16, CA-04, CA-05 e CA-07; casos TC-11, TC-12, TC-13, TC-15, TC-23 e TC-24).
 * O PUT substitui a sala por inteiro e o campo restrito é obrigatório (P-03).
 */
@TesteDeApiComBanco
class AlteracaoDeSalaApiTest {

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

    private int capacidadeGravada(long id) {
        return jdbc.queryForObject("SELECT capacidade FROM sala WHERE recurso_id = ?", Integer.class, id);
    }

    private boolean restritoGravado(long id) {
        return jdbc.queryForObject("SELECT restrito FROM recurso WHERE id = ?", Boolean.class, id);
    }

    @Test
    void tc11_alterarONomeEACapacidadeDevolve200ComOsDadosNovos() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpoValido("sala-140"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, id, corpo("sala-141", "Laboratório", "45", "Bloco B", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("sala-141"))
                .andExpect(jsonPath("$.capacidade").value(45));
    }

    @Test
    void tc12_alterarACapacidadeParaZeroDa400ENaoMudaASala() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpoValido("sala-142"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, id, corpo("sala-142", "Laboratório", "0", "Bloco B", "false"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("capacidade")));

        assertThat(capacidadeGravada(id)).isEqualTo(30);
    }

    @Test
    void tc13_alterarMantendoOProprioNomeDevolve200() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpoValido("sala-143"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, id, corpo("sala-143", "Laboratório", "50", "Bloco B", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacidade").value(50));
    }

    @Test
    void tc15_consultarOuAlterarSalaInexistenteDa404() throws Exception {
        mvc.perform(get("/api/salas/999999")
                        .header(HttpHeaders.AUTHORIZATION, IdentidadesDeTeste.bearerDe(tokens, Perfil.ADMINISTRADOR)))
                .andExpect(status().isNotFound());

        altera(mvc, tokens, Perfil.ADMINISTRADOR, 999999, corpoValido("sala-144"))
                .andExpect(status().isNotFound());
    }

    @Test
    void tc23_alterarONomeParaODeOutraSalaDa409ENaoMudaASala() throws Exception {
        long primeira = cadastraEDevolveId(mvc, tokens, corpoValido("sala-150"));
        cadastraEDevolveId(mvc, tokens, corpoValido("sala-151"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, primeira, corpo("sala-151", "Laboratório", "30", "Bloco B", "false"))
                .andExpect(status().isConflict());

        String nome = jdbc.queryForObject("SELECT nome FROM recurso WHERE id = ?", String.class, primeira);
        assertThat(nome).isEqualTo("sala-150");
    }

    @Test
    void tc24_putSemRestritoDa400ENaoTiraAExigenciaDeAprovacao() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpo("sala-145", "Laboratório", "30", "Bloco B", "true"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, id, corpo("sala-145", "Laboratório", "30", "Bloco B", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("restrito")));

        assertThat(restritoGravado(id)).isTrue();
    }
}
