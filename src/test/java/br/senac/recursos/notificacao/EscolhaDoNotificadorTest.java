package br.senac.recursos.notificacao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** A configuração escolhe um só Notificador conforme a url-base (spec T-31, CA-09; DEF-03). */
class EscolhaDoNotificadorTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(NotificacaoConfiguracao.class)
            .withPropertyValues("app.notificacao.timeout-conexao=500ms", "app.notificacao.timeout-leitura=1500ms");

    @Test
    @DisplayName("Sem url-base, o único Notificador é o NotificadorEmLog (modo simulado)")
    void semUrlBaseUsaOModoSimulado() {
        contexto.withPropertyValues("app.notificacao.url-base=").run(ctx -> {
            assertThat(ctx).hasSingleBean(Notificador.class);
            assertThat(ctx.getBean(Notificador.class)).isInstanceOf(NotificadorEmLog.class);
        });
    }

    @Test
    @DisplayName("Com url-base, o único Notificador é o NotificadorHttp")
    void comUrlBaseUsaOAdaptadorHttp() {
        contexto.withPropertyValues("app.notificacao.url-base=http://localhost:9", "app.notificacao.token=tk-falso")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(Notificador.class);
                    assertThat(ctx.getBean(Notificador.class)).isInstanceOf(NotificadorHttp.class);
                });
    }
}
