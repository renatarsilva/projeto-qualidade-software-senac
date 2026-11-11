package br.senac.recursos.notificacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * O adaptador HTTP da API externa de notificações (spec T-31, RF-11). URL, token e tempos limite vêm só da
 * configuração. Nunca lança exceção: toda falha vira {@code FALHOU} com a causa, e o log de falha não traz o token, o
 * corpo de erro do provedor nem o rastro da biblioteca HTTP (CA-07).
 */
public class NotificadorHttp implements Notificador {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorHttp.class);

    private final ObjectMapper json = JsonMapper.builder().findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
    private final RestClient cliente;
    private final String token;

    public NotificadorHttp(NotificacaoPropriedades propriedades) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(propriedades.timeoutConexao()).build());
        fabrica.setReadTimeout(propriedades.timeoutLeitura());
        this.cliente = RestClient.builder().baseUrl(propriedades.urlBase().trim()).requestFactory(fabrica).build();
        this.token = propriedades.token();
    }

    @Override
    public ResultadoDaNotificacao notifica(Notificacao notificacao) {
        try {
            ResultadoDaNotificacao resultado = cliente.post().uri("/notificacoes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsString(notificacao))
                    .exchange((requisicao, resposta) -> {
                        int status = resposta.getStatusCode().value();
                        if (status >= 200 && status < 300) {
                            return leResposta(StreamUtils.copyToString(resposta.getBody(),
                                    java.nio.charset.StandardCharsets.UTF_8));
                        }
                        return ResultadoDaNotificacao.falhou(
                                status >= 400 && status < 500 ? CausaDeFalha.HTTP_4XX : CausaDeFalha.INDISPONIVEL);
                    });
            return registra(notificacao, resultado);
        } catch (ResourceAccessException erro) {
            return registra(notificacao, ResultadoDaNotificacao.falhou(causaDe(erro)));
        } catch (Exception erro) {
            return registra(notificacao, ResultadoDaNotificacao.falhou(CausaDeFalha.INDISPONIVEL));
        }
    }

    /** O 2xx só vale com um JSON que tenha {@code id} de texto não vazio. */
    private ResultadoDaNotificacao leResposta(String corpo) {
        try {
            JsonNode raiz = json.readTree(corpo);
            JsonNode id = raiz == null ? null : raiz.get("id");
            if (id != null && id.isTextual() && !id.asText().isBlank()) {
                return ResultadoDaNotificacao.enviada(id.asText());
            }
        } catch (Exception erro) {
            // corpo que não é JSON: cai na resposta inválida
        }
        return ResultadoDaNotificacao.falhou(CausaDeFalha.RESPOSTA_INVALIDA);
    }

    private static CausaDeFalha causaDe(ResourceAccessException erro) {
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof HttpConnectTimeoutException || causa instanceof ConnectException) {
                return CausaDeFalha.CONEXAO;
            }
            if (causa instanceof HttpTimeoutException) {
                return CausaDeFalha.TIMEOUT;
            }
        }
        return CausaDeFalha.CONEXAO;
    }

    /** Exatamente uma linha de log por notificação; a de falha só traz a reserva e a causa (CA-08). */
    private ResultadoDaNotificacao registra(Notificacao notificacao, ResultadoDaNotificacao resultado) {
        if (resultado.situacao() == ResultadoDaNotificacao.Situacao.FALHOU) {
            LOG.warn("Falha na notificação da reserva {}: causa={}", notificacao.reservaId(), resultado.causa());
        } else {
            LOG.info("Notificação enviada: reserva {}, evento {}", notificacao.reservaId(), notificacao.evento());
        }
        return resultado;
    }
}
