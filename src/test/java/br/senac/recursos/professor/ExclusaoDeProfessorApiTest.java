package br.senac.recursos.professor;

import static br.senac.recursos.professor.ProfessoresDeTeste.cadastra;
import static br.senac.recursos.professor.ProfessoresDeTeste.cadastraEDevolveId;
import static br.senac.recursos.professor.ProfessoresDeTeste.corpo;
import static br.senac.recursos.professor.ProfessoresDeTeste.exclui;
import static br.senac.recursos.professor.ProfessoresDeTeste.limpaBanco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TesteDeApiComBanco;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exclusão de professor pela API (spec T-17, CA-07; casos TC-11, TC-12, TC-30 e o DELETE do TC-10).
 * O vínculo é simulado por uma tabela de apoio que existe só no teste. Os testes com reserva, retirada
 * e manutenção reais são da T-61.
 */
@TesteDeApiComBanco
class ExclusaoDeProfessorApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void preparar() {
        jdbc.execute("DROP TABLE IF EXISTS apoio_exclusao_teste");
        limpaBanco(jdbc);
    }

    @AfterEach
    void desfazerApoio() {
        jdbc.execute("DROP TABLE IF EXISTS apoio_exclusao_teste");
    }

    private long linhas(String tabela) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabela, Long.class);
    }

    @Test
    void tc11_excluirProfessorSemVinculoDa204EApagaProfessorECompetencias() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpo("ana-souza-12345", "false", "[\"Java\",\"SQL\"]"));

        exclui(mvc, tokens, Perfil.ADMINISTRADOR, id).andExpect(status().isNoContent());

        assertThat(linhas("recurso")).isZero();
        assertThat(linhas("professor")).isZero();
        assertThat(linhas("competencia")).isZero();
    }

    @Test
    void tc12_excluirProfessorComVinculoDa409EOProfessorContinua() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpo("ana-souza-12345", "false", "[\"Java\"]"));
        jdbc.execute("CREATE TABLE apoio_exclusao_teste (recurso_id BIGINT NOT NULL "
                + "REFERENCES recurso (id) ON DELETE RESTRICT)");
        jdbc.update("INSERT INTO apoio_exclusao_teste (recurso_id) VALUES (?)", id);

        exclui(mvc, tokens, Perfil.ADMINISTRADOR, id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este registro está em uso e não pode ser removido"));

        assertThat(linhas("professor")).isEqualTo(1);
        assertThat(linhas("competencia")).isEqualTo(1);
    }

    @Test
    void tc30_depoisDeExcluirOMesmoNomeEAMesmaMatriculaPodemSerCadastradosDeNovo() throws Exception {
        long id = cadastraEDevolveId(mvc, tokens, corpo("ana-souza-12345", "false", "[\"Java\"]"));
        exclui(mvc, tokens, Perfil.ADMINISTRADOR, id).andExpect(status().isNoContent());
        assertThat(linhas("recurso")).isZero();

        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpo("ana-souza-12345", "false", "[\"Java\"]"))
                .andExpect(status().isCreated());
    }

    @Test
    void tc10_excluirProfessorInexistenteDa404() throws Exception {
        exclui(mvc, tokens, Perfil.ADMINISTRADOR, 999999).andExpect(status().isNotFound());
    }
}
