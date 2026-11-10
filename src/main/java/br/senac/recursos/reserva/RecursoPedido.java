package br.senac.recursos.reserva;

/** Um recurso pedido (ou ocupado) por uma reserva. */
public record RecursoPedido(TipoDeRecurso tipo, long id) {
}
