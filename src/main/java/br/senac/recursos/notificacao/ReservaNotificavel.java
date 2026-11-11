package br.senac.recursos.notificacao;

import java.time.Instant;

/** Evento publicado quando uma reserva muda para um estado que notifica; o ouvinte envia depois do commit. */
public record ReservaNotificavel(long reservaId, EventoDeNotificacao evento, String destinatario,
        Instant ocorridaEm) {
}
