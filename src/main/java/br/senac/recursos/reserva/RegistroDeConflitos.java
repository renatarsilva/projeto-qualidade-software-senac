package br.senac.recursos.reserva;

import java.time.Instant;

/** Onde cada recusa por sobreposição deixa o seu registro, para o relatório de utilização (spec T-41, RF-12). */
public interface RegistroDeConflitos {

    /** Registra um conflito evitado no recurso, no instante da tentativa. */
    void registra(RecursoPedido recurso, Instant ocorridoEm);
}
