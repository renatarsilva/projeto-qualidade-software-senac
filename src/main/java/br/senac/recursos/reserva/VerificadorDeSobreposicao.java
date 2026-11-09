package br.senac.recursos.reserva;

import java.time.Instant;

/** A verificação de sobreposição da aplicação. É uma interface para o teste TC-58 poder contorná-la. */
public interface VerificadorDeSobreposicao {

    /** Há reserva que ocupa o recurso em algum instante de {@code [inicio, termino)}, fora a {@code ignorada}? */
    boolean ocupado(RecursoPedido recurso, Instant inicio, Instant termino, Long reservaIgnorada);
}
