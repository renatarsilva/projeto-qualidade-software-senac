package br.senac.recursos.reserva;

import java.time.Instant;
import java.util.List;

/** Uma reserva como a agenda a guarda. O {@code id} 0 significa ainda não gravada. */
public record ReservaNaAgenda(long id, long donoId, EstadoDaReserva estado, Instant inicio, Instant termino,
        List<RecursoPedido> recursos, Instant criadaEm, Instant prazoAprovacaoEm) {
}
