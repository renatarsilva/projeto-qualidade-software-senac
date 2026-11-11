package br.senac.recursos.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Spec T-44, CA-05, TC-33: no servidor de verdade o 403 das telas chega à página de erro pelo {@code sendError}, e o
 * MockMvc não faz esse encaminhamento. Este teste simula o encaminhamento e confere a página amigável (a falha foi
 * achada rodando a aplicação: o 403 virava 500).
 */
@TesteDeTelas
@DisplayName("Telas: página de acesso negado")
class PaginaDeAcessoNegadoTest extends BaseDasTelas {

    @Test
    @DisplayName("TC-33: o 403 de uma tela vira a página amigável com status 403, e não um 500")
    void paginaAmigavelNoAcessoNegado() throws Exception {
        mvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/admin/salas"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("permissão")))
                .andExpect(content().string(not(containsString("Exception"))));
    }
}
