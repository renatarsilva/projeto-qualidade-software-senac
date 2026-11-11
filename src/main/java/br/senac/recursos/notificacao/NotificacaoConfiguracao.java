package br.senac.recursos.notificacao;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Liga as propriedades de {@code app.notificacao.*}. */
@Configuration
@EnableConfigurationProperties(NotificacaoPropriedades.class)
public class NotificacaoConfiguracao {
}
