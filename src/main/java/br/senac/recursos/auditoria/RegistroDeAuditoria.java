package br.senac.recursos.auditoria;

import br.senac.recursos.reserva.EstadoDaReserva;
import java.time.Instant;

/** Um registro de auditoria (spec T-40, L07). Usuário vazio significa sistema; estado anterior vazio, criação. */
public record RegistroDeAuditoria(long id, long reservaId, Long usuarioId, EstadoDaReserva estadoAnterior,
        EstadoDaReserva estadoNovo, Instant ocorridaEm, String motivo) {
}
