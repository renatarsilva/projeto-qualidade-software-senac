package br.senac.recursos.erro;

import static org.assertj.core.api.Assertions.assertThat;

import br.senac.recursos.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * Falha em filtro (spec T-22, CA-14; casos TC-39 e TC-40). Só um contêiner de verdade encaminha a falha para /error,
 * por isso este teste sobe o servidor em uma porta (o MockMvc não faz esse encaminhamento).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, SegurancaDeTesteDeErros.class})
@TestPropertySource(properties = {
        "app.seguranca.chave-assinatura=chave-de-teste-com-pelo-menos-32-caracteres-123",
        "app.admin-inicial.email=admin-de-teste@exemplo.com",
        "app.admin-inicial.senha=senha-inicial-de-teste"})
class FalhaEmFiltroTest {

    @Autowired
    private TestRestTemplate http;

    private ResponseEntity<String> chama(String caminho, MediaType aceita) {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setAccept(java.util.List.of(aceita));
        return http.exchange(caminho, org.springframework.http.HttpMethod.GET, new HttpEntity<>(cabecalhos),
                String.class);
    }

    @Test
    void tc39_falhaEmFiltroDaApiDevolveProblemaEmJsonComTituloDoCatalogo() {
        ResponseEntity<String> resposta = chama("/api/testes-de-erro/falha-no-filtro", MediaType.APPLICATION_JSON);

        assertThat(resposta.getStatusCode().value()).isEqualTo(500);
        assertThat(resposta.getHeaders().getContentType().toString()).startsWith("application/problem+json");
        assertThat(resposta.getBody()).contains("\"title\":\"Erro inesperado\"");
        TermosProibidos.naoTem(resposta.getBody());
    }

    @Test
    void tc40_falhaEmFiltroForaDaApiMostraPaginaAmigavel() {
        ResponseEntity<String> resposta = chama("/teste-web/falha-no-filtro", MediaType.TEXT_HTML);

        assertThat(resposta.getStatusCode().value()).isEqualTo(500);
        assertThat(resposta.getHeaders().getContentType().toString()).startsWith("text/html");
        assertThat(resposta.getBody()).contains("href=\"/\"");
        TermosProibidos.naoTem(resposta.getBody());
    }
}
