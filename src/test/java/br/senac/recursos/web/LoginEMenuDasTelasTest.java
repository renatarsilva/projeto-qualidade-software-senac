package br.senac.recursos.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.seguranca.Perfil;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Spec T-44, CA-01 a CA-04: layout responsivo, login, logout, redirecionamento do anônimo e menu por perfil. */
@TesteDeTelas
@DisplayName("Telas: layout, login e menu")
class LoginEMenuDasTelasTest extends BaseDasTelas {

    private static final List<String> TODOS_OS_LINKS = List.of("/recursos", "/reservas", "/reservas/nova",
            "/aprovacoes", "/relatorio", "/admin/salas", "/admin/materiais", "/admin/professores", "/admin/usuarios",
            "/admin/manutencoes");

    @Test
    @DisplayName("TC-01: a tela de login é responsiva (viewport, Bootstrap e menu recolhível)")
    void loginResponsivo() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("<meta name=\"viewport\"")))
                .andExpect(content().string(containsString("bootstrap.min.css")));
    }

    @Test
    @DisplayName("TC-01: a página inicial tem o botão do menu para telas estreitas")
    void paginaInicialResponsiva() throws Exception {
        mvc.perform(get("/").with(comoSolicitante())).andExpect(status().isOk())
                .andExpect(content().string(containsString("<meta name=\"viewport\"")))
                .andExpect(content().string(containsString("navbar-toggler")));
    }

    @Test
    @DisplayName("TC-02: login com a senha certa entra e abre a sessão")
    void loginValido() throws Exception {
        mvc.perform(formLogin("/login").user("username", "marina" + SUFIXO).password("password", SENHA))
                .andExpect(redirectedUrl("/")).andExpect(authenticated());
    }

    @ParameterizedTest(name = "TC-03: login recusado com {0}")
    @ValueSource(strings = {"email-inexistente", "senha-errada", "desativado"})
    void loginRecusadoComAMesmaMensagem(String caso) throws Exception {
        if (caso.equals("desativado")) {
            jdbc.update("UPDATE usuario SET ativo = false WHERE email = ?", "marina" + SUFIXO);
        }
        String email = caso.equals("email-inexistente") ? "ninguem" + SUFIXO : "marina" + SUFIXO;
        String senha = caso.equals("senha-errada") ? "outra-senha-qualquer" : SENHA;
        mvc.perform(formLogin("/login").user("username", email).password("password", senha))
                .andExpect(redirectedUrl("/login?erro")).andExpect(unauthenticated());
        mvc.perform(get("/login").param("erro", ""))
                .andExpect(content().string(containsString("E-mail ou senha inválidos")));
    }

    @Test
    @DisplayName("TC-04: o logout encerra a sessão")
    void logout() throws Exception {
        MockHttpSession sessao = (MockHttpSession) mvc.perform(
                formLogin("/login").user("username", "marina" + SUFIXO).password("password", SENHA))
                .andReturn().getRequest().getSession();
        mvc.perform(post("/logout").session(sessao).with(csrf())).andExpect(redirectedUrl("/login?saiu"));
        mvc.perform(get("/reservas").session(sessao)).andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @ParameterizedTest(name = "TC-05: anônimo em {0} vai para o login")
    @ValueSource(strings = {"/", "/reservas", "/recursos", "/admin/salas", "/relatorio", "/aprovacoes"})
    void anonimoVaiParaOLogin(String rota) throws Exception {
        mvc.perform(get(rota)).andExpect(status().isFound()).andExpect(redirectedUrl("http://localhost/login"));
    }

    @ParameterizedTest(name = "TC-06: {0} continua aberta ao anônimo")
    @ValueSource(strings = {"/login", "/error", "/swagger-ui/index.html", "/v3/api-docs"})
    void rotasAbertas(String rota) throws Exception {
        int codigo = mvc.perform(get(rota)).andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(codigo).isNotIn(302, 401, 403);
    }

    @Test
    @DisplayName("TC-07: o menu do Solicitante traz só as telas dele")
    void menuDoSolicitante() throws Exception {
        confereMenu(comoSolicitante(), "/recursos", "/reservas", "/reservas/nova");
    }

    @Test
    @DisplayName("TC-07: o menu do Responsável traz só as telas dele")
    void menuDoResponsavel() throws Exception {
        confereMenu(comoResponsavel(), "/recursos", "/reservas", "/aprovacoes", "/relatorio");
    }

    @Test
    @DisplayName("TC-07: o menu do Administrador traz só as telas dele")
    void menuDoAdministrador() throws Exception {
        confereMenu(comoAdministrador(), "/recursos", "/reservas", "/relatorio", "/admin/salas", "/admin/materiais",
                "/admin/professores", "/admin/usuarios", "/admin/manutencoes");
    }

    @Test
    @DisplayName("TC-32: quem foi desativado perde o acesso da sessão na hora")
    void desativadoPerdeASessao() throws Exception {
        mvc.perform(get("/reservas").with(comoSolicitante())).andExpect(status().isOk());
        jdbc.update("UPDATE usuario SET ativo = false WHERE id = ?", solicitante);
        mvc.perform(get("/reservas").with(comoSolicitante())).andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @DisplayName("TC-32: o perfil que vale é o do banco, e não o da sessão")
    void perfilDoBancoVale() throws Exception {
        mvc.perform(get("/admin/salas").with(como(solicitante, Perfil.ADMINISTRADOR))).andExpect(status().isForbidden());
    }

    private void confereMenu(RequestPostProcessor perfil, String... permitidos) throws Exception {
        var resposta = mvc.perform(get("/").with(perfil)).andExpect(status().isOk());
        List<String> ok = List.of(permitidos);
        for (String link : TODOS_OS_LINKS) {
            String trecho = "href=\"" + link + "\"";
            resposta.andExpect(ok.contains(link) ? content().string(containsString(trecho))
                    : content().string(not(containsString(trecho))));
        }
    }
}
