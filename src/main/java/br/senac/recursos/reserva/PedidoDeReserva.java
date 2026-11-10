package br.senac.recursos.reserva;

import java.time.Instant;
import java.util.List;

/** O que o cliente pede: intervalo e recursos. Início e término podem faltar (RN-01). */
public record PedidoDeReserva(Instant inicio, Instant termino, List<RecursoPedido> recursos) {
}
