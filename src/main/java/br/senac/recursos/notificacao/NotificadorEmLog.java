package br.senac.recursos.notificacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Modo simulado: sem {@code app.notificacao.url-base}, só registra o aviso no log (RF-11 aceita "simulada"). */
public class NotificadorEmLog implements Notificador {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorEmLog.class);

    @Override
    public ResultadoDaNotificacao notifica(Notificacao notificacao) {
        LOG.info("Notificação simulada: reserva {}, evento {}", notificacao.reservaId(), notificacao.evento());
        return ResultadoDaNotificacao.enviada("simulada");
    }
}
