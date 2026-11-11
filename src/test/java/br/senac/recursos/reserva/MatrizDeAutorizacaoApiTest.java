package br.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import br.senac.recursos.erro.TermosProibidos;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;

/**
 * A matriz de autorização célula a célula, com usuários reais de cada perfil e tokens reais
 * (spec T-49, CA-01 a CA-03; TC-01 a TC-04). A fonte é {@link MatrizDeAutorizacao}.
 */
@TesteDeReservaApi
class MatrizDeAutorizacaoApiTest extends BaseDaApiDeReserva {

    static Stream<Operacao> operacoes() {
        return MatrizDeAutorizacao.OPERACOES.stream();
    }

    /** As células que a matriz marca como 403: operação e perfil. */
    static Stream<Arguments> celulasDeNegacao() {
        return celulas(403);
    }

    /** As células que a matriz marca com resposta de negócio (200, 201, 204 ou 409). */
    static Stream<Arguments> celulasComResposta() {
        return MatrizDeAutorizacao.OPERACOES.stream().filter(o -> o.chamada() != null)
                .flatMap(o -> Stream.of(Quem.SOLICITANTE, Quem.RESPONSAVEL, Quem.ADMINISTRADOR)
                        .filter(q -> o.esperado(q) != 403 && o.esperado(q) != 401)
                        .map(q -> Arguments.of(o, q)));
    }

    private static Stream<Arguments> celulas(int status) {
        return MatrizDeAutorizacao.OPERACOES.stream()
                .flatMap(o -> Stream.of(Quem.SOLICITANTE, Quem.RESPONSAVEL, Quem.ADMINISTRADOR)
                        .filter(q -> o.esperado(q) == status).map(q -> Arguments.of(o, q)));
    }

    private ResultActions pedidoFicticio(Operacao operacao, String bearer) throws Exception {
        return ambiente.chama(operacao.metodo(), operacao.caminhoFicticio(), bearer, operacao.corpoFicticio());
    }

    private static void semDetalhesInternos(ResultActions resultado) throws Exception {
        MockHttpServletResponse resposta = resultado.andReturn().getResponse();
        TermosProibidos.naoTem(resposta.getContentAsString());
    }

    @ParameterizedTest(name = "TC-01: {0} sem token devolve 401")
    @MethodSource("operacoes")
    @DisplayName("TC-01: toda operação da matriz, sem token, devolve 401")
    void semToken(Operacao operacao) throws Exception {
        ResultActions resultado = pedidoFicticio(operacao, null);

        assertThat(resultado.andReturn().getResponse().getStatus()).as("status").isEqualTo(operacao.anonimo());
        semDetalhesInternos(resultado);
    }

    @ParameterizedTest(name = "TC-02: {0} com token adulterado devolve 401")
    @MethodSource("operacoes")
    @DisplayName("TC-02: toda operação da matriz, com token adulterado, devolve 401")
    void tokenAdulterado(Operacao operacao) throws Exception {
        String adulterado = ambiente.bearerDe(br.senac.recursos.seguranca.Perfil.ADMINISTRADOR) + "x";

        ResultActions resultado = pedidoFicticio(operacao, adulterado);

        assertThat(resultado.andReturn().getResponse().getStatus()).as("status").isEqualTo(401);
    }

    @ParameterizedTest(name = "TC-03: {0} por {1} devolve 403")
    @MethodSource("celulasDeNegacao")
    @DisplayName("TC-03: cada célula 403 da matriz devolve 403, sem detalhes internos")
    void celulaDeNegacao(Operacao operacao, Quem quem) throws Exception {
        ResultActions resultado = pedidoFicticio(operacao, ambiente.bearerDe(quem.perfil));

        assertThat(resultado.andReturn().getResponse().getStatus()).as("status").isEqualTo(403);
        semDetalhesInternos(resultado);
    }

    @ParameterizedTest(name = "TC-04: {0} por {1} devolve o status da matriz")
    @MethodSource("celulasComResposta")
    @DisplayName("TC-04: cada célula de sucesso da matriz devolve o status esperado, com dados novos")
    void celulaDeSucesso(Operacao operacao, Quem quem) throws Exception {
        String bearer = quem == Quem.SOLICITANTE ? ambiente.bearerDoSolicitante() : ambiente.bearerDe(quem.perfil);
        CenarioDeAutorizacao cenario = new CenarioDeAutorizacao(ambiente, relogio, jdbc, sala, bearer);

        ResultActions resultado = operacao.chamada().apply(cenario);

        assertThat(resultado.andReturn().getResponse().getStatus()).as("status de %s por %s", operacao, quem)
                .isEqualTo(operacao.esperado(quem));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("A-01: o login é público: credencial inválida devolve 401 genérico, e não 403 nem 500")
    void loginEPublico() throws Exception {
        ResultActions resultado = ambiente.chama(HttpMethod.POST, MatrizDeAutorizacao.LOGIN, null,
                "{\"email\":\"nao-existe@exemplo.com\",\"senha\":\"senha-qualquer-1\"}");

        assertThat(resultado.andReturn().getResponse().getStatus()).isEqualTo(401);
        semDetalhesInternos(resultado);
    }
}
