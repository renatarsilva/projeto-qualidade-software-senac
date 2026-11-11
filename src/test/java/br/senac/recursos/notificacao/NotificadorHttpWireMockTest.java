package br.senac.recursos.notificacao;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import br.senac.recursos.notificacao.ResultadoDaNotificacao.Situacao;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * O adaptador HTTP contra uma API externa simulada pelo WireMock, em processo e em porta livre: roda offline e no CI
 * (spec T-31, CA-01 a CA-08; TC-01 a TC-12 e TC-15). O token é falso e criado no próprio teste.
 */
@ExtendWith(OutputCaptureExtension.class)
class NotificadorHttpWireMockTest {

    private static final String TOKEN = "tk-falso-123";
    private static final Notificacao NOTIFICACAO = new Notificacao(3, EventoDeNotificacao.APROVADA,
            "marina@senac.br", Instant.parse("2026-11-12T12:00:00Z"));
    private static WireMockServer wiremock;

    @BeforeAll
    static void iniciaOServidor() {
        wiremock = new WireMockServer(options().dynamicPort());
        wiremock.start();
    }

    @AfterAll
    static void paraOServidor() {
        wiremock.stop();
    }

    @BeforeEach
    void zeraOsStubs() {
        wiremock.resetAll();
    }

    private static NotificadorHttp notificador(String urlBase, Duration leitura) {
        return new NotificadorHttp(new NotificacaoPropriedades(urlBase, TOKEN, Duration.ofMillis(500), leitura));
    }

    private static NotificadorHttp notificador() {
        return notificador("http://localhost:" + wiremock.port(), Duration.ofMillis(1500));
    }

    private static void responde(int status, String corpo) {
        wiremock.stubFor(post(urlEqualTo("/notificacoes")).willReturn(aResponse().withStatus(status)
                .withHeader("Content-Type", "application/json").withBody(corpo)));
    }

    private static void assertFalhou(ResultadoDaNotificacao resultado, CausaDeFalha causa) {
        assertThat(resultado.situacao()).as("situação").isEqualTo(Situacao.FALHOU);
        assertThat(resultado.causa()).as("causa").isEqualTo(causa);
    }

    @Test
    @DisplayName("TC-01 (INT-01): 200 com id válido é ENVIADA, e o WireMock recebe 1 POST com o JSON do contrato e o Bearer")
    void envioComSucesso() {
        responde(200, "{\"id\":\"n-1\"}");

        ResultadoDaNotificacao resultado = notificador().notifica(NOTIFICACAO);

        assertThat(resultado.situacao()).isEqualTo(Situacao.ENVIADA);
        assertThat(resultado.id()).isEqualTo("n-1");
        wiremock.verify(1, postRequestedFor(urlEqualTo("/notificacoes"))
                .withHeader("Authorization", equalTo("Bearer " + TOKEN))
                .withHeader("Content-Type", com.github.tomakehurst.wiremock.client.WireMock.containing("application/json"))
                .withRequestBody(equalToJson("{\"reservaId\":3,\"evento\":\"APROVADA\","
                        + "\"destinatario\":\"marina@senac.br\",\"ocorridaEm\":\"2026-11-12T12:00:00Z\"}")));
    }

    @ParameterizedTest(name = "TC-02: {0} com id válido é ENVIADA")
    @ValueSource(ints = {201, 202})
    @DisplayName("TC-02: qualquer 2xx com id válido é ENVIADA")
    void qualquer2xxValido(int status) {
        responde(status, "{\"id\":\"n-1\"}");

        assertThat(notificador().notifica(NOTIFICACAO).situacao()).isEqualTo(Situacao.ENVIADA);
    }

    @Test
    @DisplayName("TC-03 (INT-02): 404 vira FALHOU com a causa HTTP_4XX, sem exceção")
    void erroDoCliente() {
        responde(404, "{\"erro\":\"nao encontrado\"}");

        assertFalhou(notificador().notifica(NOTIFICACAO), CausaDeFalha.HTTP_4XX);
    }

    @ParameterizedTest(name = "TC-04 e TC-05: {0} vira INDISPONIVEL")
    @ValueSource(ints = {500, 503, 599})
    @DisplayName("TC-04 e TC-05 (INT-03): 5xx vira FALHOU com a causa INDISPONIVEL, sem exceção")
    void erroDoProvedor(int status) {
        responde(status, "{\"erro\":\"falha interna\"}");

        assertFalhou(notificador().notifica(NOTIFICACAO), CausaDeFalha.INDISPONIVEL);
    }

    @Test
    @DisplayName("TC-06 (INT-04): atraso de 2000 ms com leitura de 1500 ms vira TIMEOUT, sem esperar a resposta atrasada")
    void timeoutDeLeitura() {
        wiremock.stubFor(post(urlEqualTo("/notificacoes")).willReturn(aResponse().withStatus(200)
                .withBody("{\"id\":\"n-1\"}").withFixedDelay(2000)));

        long inicio = System.nanoTime();
        ResultadoDaNotificacao resultado = notificador().notifica(NOTIFICACAO);
        long duracaoEmMs = Duration.ofNanos(System.nanoTime() - inicio).toMillis();

        assertFalhou(resultado, CausaDeFalha.TIMEOUT);
        assertThat(duracaoEmMs).as("duração da chamada em ms").isLessThan(2000);
    }

    @Test
    @DisplayName("TC-07: atraso de 1000 ms com leitura de 1500 ms é ENVIADA (dentro do limite)")
    void dentroDoLimiteDeLeitura() {
        wiremock.stubFor(post(urlEqualTo("/notificacoes")).willReturn(aResponse().withStatus(200)
                .withBody("{\"id\":\"n-1\"}").withFixedDelay(1000)));

        assertThat(notificador().notifica(NOTIFICACAO).situacao()).isEqualTo(Situacao.ENVIADA);
    }

    @Test
    @DisplayName("TC-12: o tempo limite vem da configuração: com leitura de 500 ms, o atraso de 1000 ms vira TIMEOUT")
    void tempoLimiteVemDaConfiguracao() {
        wiremock.stubFor(post(urlEqualTo("/notificacoes")).willReturn(aResponse().withStatus(200)
                .withBody("{\"id\":\"n-1\"}").withFixedDelay(1000)));

        ResultadoDaNotificacao resultado = notificador("http://localhost:" + wiremock.port(),
                Duration.ofMillis(500)).notifica(NOTIFICACAO);

        assertFalhou(resultado, CausaDeFalha.TIMEOUT);
    }

    @Test
    @DisplayName("TC-08 (INT-05): 200 com JSON malformado vira RESPOSTA_INVALIDA")
    void jsonMalformado() {
        responde(200, "{id: ");

        assertFalhou(notificador().notifica(NOTIFICACAO), CausaDeFalha.RESPOSTA_INVALIDA);
    }

    @ParameterizedTest(name = "TC-09: corpo {0} vira RESPOSTA_INVALIDA")
    @ValueSource(strings = {"{}", "{\"id\":\"\"}", "{\"id\":\"   \"}"})
    @DisplayName("TC-09: 200 sem id ou com id vazio vira RESPOSTA_INVALIDA")
    void semIdValido(String corpo) {
        responde(200, corpo);

        assertFalhou(notificador().notifica(NOTIFICACAO), CausaDeFalha.RESPOSTA_INVALIDA);
    }

    private static int portaFechada() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Test
    @DisplayName("TC-10: nada escutando na URL configurada vira FALHOU com a causa CONEXAO")
    void conexaoRecusada() throws IOException {
        ResultadoDaNotificacao resultado = notificador("http://localhost:" + portaFechada(),
                Duration.ofMillis(1500)).notifica(NOTIFICACAO);

        assertFalhou(resultado, CausaDeFalha.CONEXAO);
    }

    @Test
    @DisplayName("TC-11: 500 com o token e um stack trace no corpo: nem o resultado nem o log vazam token, corpo ou classe")
    void naoVazaOTokenNemOErroDoProvedor(CapturedOutput saida) {
        String corpoDoProvedor = "{\"erro\":\"token tk-falso-123 inválido\","
                + "\"trace\":\"java.lang.IllegalStateException at br.provedor.Falha\"}";
        responde(500, corpoDoProvedor);

        ResultadoDaNotificacao resultado = notificador().notifica(NOTIFICACAO);

        assertFalhou(resultado, CausaDeFalha.INDISPONIVEL);
        assertThat(resultado.toString()).doesNotContain(TOKEN);
        assertThat(saida.getAll()).doesNotContain(TOKEN, "IllegalStateException", "br.provedor.Falha",
                "inválido", "RestClient", "HttpServerErrorException");
    }

    static Stream<Arguments> falhas() {
        return Stream.of(
                Arguments.of("404", 404, "{}", 0, CausaDeFalha.HTTP_4XX),
                Arguments.of("500", 500, "{}", 0, CausaDeFalha.INDISPONIVEL),
                Arguments.of("atraso de 2000 ms", 200, "{\"id\":\"n-1\"}", 2000, CausaDeFalha.TIMEOUT),
                Arguments.of("JSON malformado", 200, "{id: ", 0, CausaDeFalha.RESPOSTA_INVALIDA));
    }

    @ParameterizedTest(name = "TC-15: falha por {0}: exatamente 1 linha de log com a reserva e a causa")
    @MethodSource("falhas")
    @DisplayName("TC-15: cada falha deixa exatamente 1 linha de log, com o id da reserva e a causa")
    void umRegistroPorFalha(String descricao, int status, String corpo, int atrasoEmMs, CausaDeFalha causa,
            CapturedOutput saida) {
        wiremock.stubFor(post(urlEqualTo("/notificacoes")).willReturn(aResponse().withStatus(status)
                .withBody(corpo).withFixedDelay(atrasoEmMs)));

        ResultadoDaNotificacao resultado = notificador().notifica(NOTIFICACAO);

        assertFalhou(resultado, causa);
        long linhas = Arrays.stream(saida.getAll().split("\\R")).filter(l -> l.contains("Falha na notificação"))
                .count();
        assertThat(linhas).as("linhas de falha no log").isEqualTo(1);
        assertThat(saida.getAll()).contains("reserva 3").contains("causa=" + causa.name());
    }

    @Test
    @DisplayName("TC-14: EventoDeNotificacao tem exatamente os três eventos da DP-03")
    void soTresEventosNotificam() {
        assertThat(EventoDeNotificacao.values()).containsExactlyInAnyOrder(EventoDeNotificacao.APROVADA,
                EventoDeNotificacao.REJEITADA, EventoDeNotificacao.CANCELADA);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> EventoDeNotificacao.valueOf("EM_USO"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Pattern.compile("x").matcher("x").matches()).isTrue();
    }
}
