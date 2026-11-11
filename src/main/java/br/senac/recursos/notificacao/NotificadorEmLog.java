package br.senac.recursos.notificacao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Modo simulado: sem {@code app.notificacao.url-base}, só registra o aviso no log. Esqueleto da T-31. */
@Component
@ConditionalOnProperty(prefix = "app.notificacao", name = "url-base", havingValue = "", matchIfMissing = true)
public class NotificadorEmLog implements Notificador {

    @Override
    public ResultadoDaNotificacao notifica(Notificacao notificacao) {
        return ResultadoDaNotificacao.pendente();
    }
}
