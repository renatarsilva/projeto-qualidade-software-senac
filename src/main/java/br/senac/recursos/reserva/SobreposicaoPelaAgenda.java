package br.senac.recursos.reserva;

import java.time.Instant;

/** Verifica a sobreposição lendo as reservas da agenda (usada nos testes unitários; a aplicação usa o banco). */
public class SobreposicaoPelaAgenda implements VerificadorDeSobreposicao {

    private final AgendaDeReservas agenda;

    public SobreposicaoPelaAgenda(AgendaDeReservas agenda) {
        this.agenda = agenda;
    }

    @Override
    public boolean ocupado(RecursoPedido recurso, Instant inicio, Instant termino, Long reservaIgnorada) {
        // Período [início, fim): encostar não é sobrepor (S-01).
        return agenda.reservas().stream()
                .filter(r -> r.estado().ocupaHorario())
                .filter(r -> reservaIgnorada == null || r.id() != reservaIgnorada)
                .filter(r -> r.recursos().contains(recurso))
                .anyMatch(r -> r.inicio().isBefore(termino) && inicio.isBefore(r.termino()));
    }
}
