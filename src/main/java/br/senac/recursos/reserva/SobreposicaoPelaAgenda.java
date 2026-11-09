package br.senac.recursos.reserva;

import java.time.Instant;

/** Verifica a sobreposição lendo as reservas da agenda. Assinatura mínima da T-20: a lógica é da T-23. */
public class SobreposicaoPelaAgenda implements VerificadorDeSobreposicao {

    private final AgendaDeReservas agenda;

    public SobreposicaoPelaAgenda(AgendaDeReservas agenda) {
        this.agenda = agenda;
    }

    @Override
    public boolean ocupado(RecursoPedido recurso, Instant inicio, Instant termino, Long reservaIgnorada) {
        return false;
    }
}
