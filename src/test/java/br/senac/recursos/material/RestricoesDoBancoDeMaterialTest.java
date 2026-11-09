package br.senac.recursos.material;

import static br.senac.recursos.material.MateriaisDeTeste.cadastra;
import static br.senac.recursos.material.MateriaisDeTeste.corpoValido;
import static br.senac.recursos.material.MateriaisDeTeste.limpaBanco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TesteDeApiComBanco;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O banco é a última linha de defesa (spec T-18, CA-09; casos TC-16, TC-17 e TC-22).
 * Os INSERTs vão direto no banco, sem passar pela aplicação.
 */
@TesteDeApiComBanco
class RestricoesDoBancoDeMaterialTest {

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

    private long insereRecurso(String tipo, String nome) {
        return jdbc.queryForObject(
                "INSERT INTO recurso (tipo, nome, restrito) VALUES (?, ?, false) RETURNING id", Long.class, tipo, nome);
    }

    private void insereMaterial(String nome, String patrimonio) {
        long recurso = insereRecurso("MATERIAL", nome);
        jdbc.update("INSERT INTO material (recurso_id, tipo_material, patrimonio) VALUES (?, 'Projetor', ?)",
                recurso, patrimonio);
    }

    @Test
    void tc16_gravarUmMaterialCriaUmaLinhaEmRecursoEUmaEmMaterial() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpoValido("Projetor Epson", "PAT-1"))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM recurso WHERE tipo = 'MATERIAL'", Long.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM material", Long.class)).isEqualTo(1);
    }

    @Test
    void tc17_patrimonioRepetidoSemDiferenciarMaiusculasERecusadoPeloIndiceUnico() {
        insereMaterial("Projetor A", "pat-1");

        assertThatThrownBy(() -> insereMaterial("Projetor B", "PAT-1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tc22_mesmoTipoEMesmoNomeEmMaiusculasDiferentesSaoRecusadosPeloIndiceDeRecurso() {
        insereRecurso("MATERIAL", "Projetor Epson");

        assertThatThrownBy(() -> insereRecurso("MATERIAL", "PROJETOR EPSON"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
