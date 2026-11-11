package br.senac.recursos.notificacao;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuração da notificação externa: tudo vem de {@code app.notificacao.*} (DP-08, MC-04 e MC-05). */
@ConfigurationProperties(prefix = "app.notificacao")
public record NotificacaoPropriedades(String urlBase, String token, Duration timeoutConexao,
        Duration timeoutLeitura) {
}
