package br.senac.recursos.reserva;

import java.util.List;
import java.util.Optional;

/** O que a regra de reserva precisa saber e gravar. A implementação com banco é da T-23. */
public interface AgendaDeReservas {

    boolean recursoExiste(RecursoPedido recurso);

    /** Todas as reservas, em qualquer estado. */
    List<ReservaNaAgenda> reservas();

    Optional<ReservaNaAgenda> busca(long id);

    /** Grava a reserva (id 0 é nova) e devolve como ficou gravada. */
    ReservaNaAgenda grava(ReservaNaAgenda reserva);
}
