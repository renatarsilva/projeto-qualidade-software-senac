package br.senac.recursos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Login pela API (spec T-15, CA-01 a CA-04; casos TC-06 a TC-10).
 * Banco real (Testcontainers). A chave abaixo é só de teste: a de verdade vem do ambiente (CA-13).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "app.seguranca.chave-assinatura=chave-de-teste-com-pelo-menos-32-caracteres-123")
class LoginApiTest {

    private static final String SENHA = "senha-de-teste-1";
    private static final String MENSAGEM_PADRAO = "E-mail ou senha inválidos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final CodificadorDeSenha codificador = new CodificadorDeSenha();

    @BeforeEach
    void prepararUsuarios() {
        jdbc.update("DELETE FROM usuario");
        inserirUsuario("marina@exemplo.com", true);
        inserirUsuario("desativado@exemplo.com", false);
    }

    private void inserirUsuario(String email, boolean ativo) {
        jdbc.update(
                "INSERT INTO usuario (nome, email, senha_hash, perfil, ativo) VALUES (?, ?, ?, 'SOLICITANTE', ?)",
                "Usuário de teste", email, codificador.codifica(SENHA), ativo);
    }

    private String login(String corpoJson, int statusEsperado) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoJson))
                .andExpect(status().is(statusEsperado))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void tc06_loginCorretoDevolveTokenTipoEExpiracao() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"marina@exemplo.com\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").isNotEmpty());
    }

    @Test
    void tc07_senhaErradaDa401ComMensagemPadrao() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"marina@exemplo.com\",\"senha\":\"senha-errada-123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(MENSAGEM_PADRAO));
    }

    @Test
    void tc08_emailInexistenteDa401ComOMesmoCorpoDaSenhaErrada() throws Exception {
        String senhaErrada = login("{\"email\":\"marina@exemplo.com\",\"senha\":\"senha-errada-123\"}", 401);
        String emailInexistente = login("{\"email\":\"nao-existe@exemplo.com\",\"senha\":\"" + SENHA + "\"}", 401);

        assertThat(emailInexistente).isNotBlank();
        assertThat(emailInexistente).isEqualTo(senhaErrada);
    }

    @Test
    void tc09_usuarioDesativadoDa401ComAMesmaMensagem() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"desativado@exemplo.com\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(MENSAGEM_PADRAO));
    }

    @Test
    void tc10_loginSemEmailOuSemSenhaDa400ApontandoOCampo() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("email")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"marina@exemplo.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("senha")));
    }
}
