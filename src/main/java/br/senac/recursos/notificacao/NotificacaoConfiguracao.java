package br.senac.recursos.notificacao;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Liga as propriedades de {@code app.notificacao.*} e escolhe o {@link Notificador}: sem {@code url-base}, o modo
 * simulado em log; com ela, o adaptador HTTP. A escolha fica em um só bean, e não em dois {@code @Component}
 * condicionais, para a injeção nunca ser ambígua (SonarCloud S9352).
 */
@Configuration
@EnableConfigurationProperties(NotificacaoPropriedades.class)
public class NotificacaoConfiguracao {

    @Bean
    Notificador notificador(NotificacaoPropriedades propriedades) {
        String urlBase = propriedades.urlBase();
        return urlBase == null || urlBase.isBlank() ? new NotificadorEmLog() : new NotificadorHttp(propriedades);
    }
}
