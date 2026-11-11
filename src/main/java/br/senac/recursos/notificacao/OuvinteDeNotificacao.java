package br.senac.recursos.notificacao;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Recebe {@link ReservaNotificavel} <b>depois do commit</b> e chama o {@link Notificador}. Se a transação é desfeita,
 * nada é enviado; se a notificação falha, a reserva já está gravada (RNF-10, spec T-31, CA-10).
 */
@Component
public class OuvinteDeNotificacao {

    private final Notificador notificador;

    public OuvinteDeNotificacao(Notificador notificador) {
        this.notificador = notificador;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoReservaNotificavel(ReservaNotificavel evento) {
        notificador.notifica(new Notificacao(evento.reservaId(), evento.evento(), evento.destinatario(),
                evento.ocorridaEm()));
    }
}
