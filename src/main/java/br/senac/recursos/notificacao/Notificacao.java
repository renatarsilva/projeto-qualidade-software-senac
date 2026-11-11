package br.senac.recursos.notificacao;

import java.time.Instant;

/** O aviso a enviar: reserva, evento, destinatário e o instante da mudança. */
public record Notificacao(long reservaId, EventoDeNotificacao evento, String destinatario, Instant ocorridaEm) {
}
