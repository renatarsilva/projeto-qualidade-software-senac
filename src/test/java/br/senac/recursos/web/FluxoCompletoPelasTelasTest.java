package br.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Spec T-44, CA-15: o fluxo principal só por formulários, com login de verdade, sessão e o token CSRF lido da própria
 * página (como faria o navegador). Sem navegador real: ver a seção "Fora" da spec.
 */
@TesteDeTelas
@DisplayName("Fluxo completo pelas telas (login, sessão e CSRF)")
class FluxoCompletoPelasTelasTest extends BaseDasTelas {

    private static final Pattern TOKEN = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @Test
    @DisplayName("TC-31: o Solicitante reserva, o Responsável aprova e o Solicitante cancela: 3 registros de auditoria")
    void fluxoCompleto() throws Exception {
        long sala = sala("sala-102", 30, "Bloco B");
        MockHttpSession marina = entra("marina" + SUFIXO);
        MockHttpSession carlos = entra("carlos" + SUFIXO);

        String token = tokenDe(marina, "/reservas/nova");
        mvc.perform(post("/reservas").session(marina).param("_csrf", token).param("inicio", "2026-11-10T10:00")
                .param("termino", "2026-11-10T11:00").param("recursos", "SALA:" + sala))
                .andExpect(status().isFound());
        long reserva = ultimaReserva();

        mvc.perform(get("/aprovacoes").session(carlos)).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"reserva-" + reserva + "\"")));
        mvc.perform(post("/reservas/" + reserva + "/aprovar").session(carlos)
                .param("_csrf", tokenDe(carlos, "/aprovacoes"))).andExpect(status().isFound());
        assertThat(estadoDa(reserva)).isEqualTo("APROVADA");

        mvc.perform(post("/reservas/" + reserva + "/cancelar").session(marina)
                .param("_csrf", tokenDe(marina, "/reservas/" + reserva))).andExpect(status().isFound());
        assertThat(estadoDa(reserva)).isEqualTo("CANCELADA");

        List<String> estados = jdbc.queryForList(
                "SELECT estado_novo FROM auditoria WHERE reserva_id = ? ORDER BY id", String.class, reserva);
        assertThat(estados).containsExactly("SOLICITADA", "APROVADA", "CANCELADA");
    }

    private MockHttpSession entra(String email) throws Exception {
        MvcResult pagina = mvc.perform(get("/login")).andReturn();
        String token = extrai(pagina.getResponse().getContentAsString());
        Cookie[] cookies = pagina.getResponse().getCookies();
        MvcResult login = mvc.perform(post("/login").param("username", email).param("password", SENHA)
                .param("_csrf", token).session((MockHttpSession) pagina.getRequest().getSession(true)))
                .andExpect(status().isFound()).andReturn();
        assertThat(cookies).isNotNull();
        return (MockHttpSession) login.getRequest().getSession();
    }

    private String tokenDe(MockHttpSession sessao, String pagina) throws Exception {
        return extrai(mvc.perform(get(pagina).session(sessao)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString());
    }

    private static String extrai(String html) {
        Matcher achou = TOKEN.matcher(html);
        assertThat(achou.find()).as("a página precisa trazer o token CSRF no formulário").isTrue();
        return achou.group(1);
    }
}
