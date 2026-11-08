package br.senac.recursos.professor;

import static br.senac.recursos.professor.ProfessoresDeTeste.altera;
import static br.senac.recursos.professor.ProfessoresDeTeste.cadastra;
import static br.senac.recursos.professor.ProfessoresDeTeste.cadastraEDevolveId;
import static br.senac.recursos.professor.ProfessoresDeTeste.corpo;
import static br.senac.recursos.professor.ProfessoresDeTeste.corpoValido;
import static br.senac.recursos.professor.ProfessoresDeTeste.limpaBanco;
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
 * Nome e matrícula únicos (spec T-17, CA-10 e CA-11; casos TC-16, TC-17, TC-23 e TC-40).
 */
@TesteDeApiComBanco
class NomeEMatriculaUnicosApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private ProfessorRepository professores;

    @BeforeEach
    void limpar() {
        limpaBanco(jdbc);
    }

    private long totalDeProfessores() {
        return jdbc.queryForObject("SELECT count(*) FROM professor", Long.class);
    }

    @Test
    void tc16_cadastrarOMesmoNomeDa409ENadaEGravado() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpoValido("ana-souza-12345")).andExpect(status().isCreated());

        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpoValido("ana-souza-12345"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um professor com esse nome"));

        assertThat(totalDeProfessores()).isEqualTo(1);
    }

    @Test
    void tc17_alterarONomeParaODeOutroProfessorDa409() throws Exception {
        cadastraEDevolveId(mvc, tokens, corpoValido("ana-souza-11111"));
        long segundo = cadastraEDevolveId(mvc, tokens, corpoValido("bia-lima-22222"));

        altera(mvc, tokens, Perfil.ADMINISTRADOR, segundo, corpo("ana-souza-11111", "false", "[]"))
                .andExpect(status().isConflict());

        String nome = jdbc.queryForObject("SELECT nome FROM recurso WHERE id = ?", String.class, segundo);
        assertThat(nome).isEqualTo("bia-lima-22222");
    }

    @Test
    void tc23_nomesDiferentesComAMesmaMatriculaDao409AMatriculaRepetida() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpoValido("ana-souza-12345")).andExpect(status().isCreated());

        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpoValido("bia-lima-12345"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um professor com essa matrícula"));

        assertThat(totalDeProfessores()).isEqualTo(1);
    }

    @Test
    void tc40_corridaSimuladaOBancoBarraComAMensagemFixaENuncaDa500() throws Exception {
        // O professor entra direto no banco, e a verificação do serviço é burlada: só o banco decide.
        long recurso = jdbc.queryForObject(
                "INSERT INTO recurso (tipo, nome, restrito) VALUES ('PROFESSOR', 'ana-souza-12345', false) RETURNING id",
                Long.class);
        jdbc.update("INSERT INTO professor (recurso_id, matricula) VALUES (?, '12345')", recurso);
        doReturn(false).when(professores).existsByNomeIgnoreCase(anyString());
        doReturn(false).when(professores).existsByMatricula(anyString());

        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpoValido("ana-souza-12345"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta operação conflita com outro registro"));

        assertThat(totalDeProfessores()).isEqualTo(1);
    }
}
