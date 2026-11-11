package br.senac.recursos.reserva;

import java.time.Instant;

/** A verificação de manutenção da reserva (RN-04, spec T-35): o recurso tem manutenção em algum instante do período? */
public interface VerificadorDeManutencao {

    boolean emManutencao(RecursoPedido recurso, Instant inicio, Instant termino);
}
