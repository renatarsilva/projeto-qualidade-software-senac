package br.senac.recursos.notificacao;

import org.springframework.stereotype.Component;

/** Recebe {@link ReservaNotificavel} depois do commit e chama o {@link Notificador}. Esqueleto da T-31. */
@Component
public class OuvinteDeNotificacao {

    private final Notificador notificador;

    public OuvinteDeNotificacao(Notificador notificador) {
        this.notificador = notificador;
    }

    public void aoReservaNotificavel(ReservaNotificavel evento) {
        // T-31 (green)
    }
}
