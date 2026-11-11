package br.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Documentação da API com OpenAPI (RF-14, T-51): o Swagger UI abre com a aplicação rodando, e todo endpoint da matriz
 * de autorização está descrito, com o resumo, os perfis permitidos, as respostas de erro e o esquema do token.
 */
@TesteDeReservaApi
class DocumentacaoDaApiTest extends BaseDaApiDeReserva {

    private String documento() throws Exception {
        MockHttpServletResponse resposta = ambiente.chama(HttpMethod.GET, "/v3/api-docs", null, null).andReturn()
                .getResponse();
        assertThat(resposta.getStatus()).as("GET /v3/api-docs sem token").isEqualTo(200);
        return resposta.getContentAsString();
    }

    @Test
    @DisplayName("O Swagger UI abre sem token com a aplicação rodando")
    void swaggerUiAbre() throws Exception {
        int status = ambiente.chama(HttpMethod.GET, "/swagger-ui/index.html", null, null).andReturn().getResponse()
                .getStatus();

        assertThat(status).isEqualTo(200);
    }

    @Test
    @DisplayName("O documento OpenAPI declara o esquema do token Bearer e o título da API")
    void esquemaDoToken() throws Exception {
        String doc = documento();

        assertThat((String) JsonPath.read(doc, "$.components.securitySchemes.bearerAuth.scheme")).isEqualTo("bearer");
        assertThat((String) JsonPath.read(doc, "$.info.title")).isNotBlank();
    }

    @ParameterizedTest(name = "{0} está documentada")
    @MethodSource("br.senac.recursos.reserva.MatrizDeAutorizacaoApiTest#operacoes")
    @DisplayName("Todo endpoint da matriz de autorização está descrito: resumo, perfis permitidos e resposta 401")
    void todoEndpointEstaDescrito(Operacao operacao) throws Exception {
        String doc = documento();
        String base = "$.paths['" + operacao.modelo() + "']." + operacao.metodo().name().toLowerCase();

        assertThat((String) JsonPath.read(doc, base + ".summary")).as("resumo de %s", operacao).isNotBlank();
        assertThat((String) JsonPath.read(doc, base + ".description")).as("descrição de %s", operacao)
                .contains("Perfis permitidos");
        assertThat((Object) JsonPath.read(doc, base + ".responses.401")).as("resposta 401 de %s", operacao).isNotNull();
    }

    @Test
    @DisplayName("O login está documentado como público, sem exigir token")
    void loginPublicoDocumentado() throws Exception {
        String doc = documento();

        assertThat((String) JsonPath.read(doc, "$.paths['/api/auth/login'].post.summary")).isNotBlank();
        assertThat((String) JsonPath.read(doc, "$.paths['/api/auth/login'].post.description")).contains("Público");
    }

    @Test
    @DisplayName("O README aponta para o Swagger UI e para o documento OpenAPI")
    void readmeTemOLink() throws Exception {
        String readme = Files.readString(Path.of("README.md"));

        assertThat(readme).contains("swagger-ui").contains("/v3/api-docs");
    }
}
