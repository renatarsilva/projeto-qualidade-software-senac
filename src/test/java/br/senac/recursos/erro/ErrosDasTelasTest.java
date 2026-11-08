package br.senac.recursos.erro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import br.senac.recursos.TesteDeApiComBanco;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Página de erro amigável nas telas (spec T-22, CA-05 e CA-14; casos TC-10, TC-12, TC-13 e TC-38; o TC-14 está em FalhaEmFiltroTest).
 * Em português, com um link de volta para a página inicial, nunca em JSON e sem nenhum termo da lista única.
 */
@TesteDeApiComBanco
@Import(SegurancaDeTesteDeErros.class)
class ErrosDasTelasTest {

    @Autowired
    private MockMvc mvc;

    private MvcResult acessa(MockHttpServletRequestBuilder requisicao) throws Exception {
        try {
            return mvc.perform(requisicao).andReturn();
        } catch (ServletException erro) {
            throw new AssertionError("A tela deixou escapar uma exceção em vez de mostrar a página de erro: "
                    + erro.getMessage(), erro);
        }
    }

    private void confereQueEUmaPaginaAmigavel(MvcResult resultado, int statusEsperado) throws Exception {
        String corpo = resultado.getResponse().getContentAsString();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(statusEsperado);
        assertThat(resultado.getResponse().getContentType()).startsWith(MediaType.TEXT_HTML_VALUE);
        assertThat(corpo).contains("href=\"/\""); // o link de volta para a página inicial
        TermosProibidos.naoTem(corpo);
    }

    @Test
    void tc10_falhaNaTelaMostraPaginaAmigavel() throws Exception {
        confereQueEUmaPaginaAmigavel(acessa(get("/teste-web/falha").accept(MediaType.TEXT_HTML)), 500);
    }

    @Test
    void tc12_rotaDeTelaQueNaoExisteMostraPaginaAmigavelComLinkParaOInicio() throws Exception {
        confereQueEUmaPaginaAmigavel(acessa(get("/pagina-que-nao-existe").accept(MediaType.TEXT_HTML)), 404);
    }

    @Test
    void tc13_conflitoNaTelaMostraPaginaAmigavelENuncaJson() throws Exception {
        confereQueEUmaPaginaAmigavel(acessa(get("/teste-web/conflito").accept(MediaType.TEXT_HTML)), 409);
    }

    @Test
    void tc38_rotaDeTelaQueNaoExisteComAcceptJsonContinuaSendoHtml() throws Exception {
        confereQueEUmaPaginaAmigavel(acessa(get("/pagina-que-nao-existe").accept(MediaType.APPLICATION_JSON)), 404);
    }
}
