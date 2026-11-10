package br.senac.recursos.reserva;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Agenda dos testes unitários: só guarda; nenhuma regra de reserva mora aqui. */
final class AgendaEmMemoria implements AgendaDeReservas {

    private final Set<RecursoPedido> recursos = new HashSet<>();
    private final List<ReservaNaAgenda> reservas = new ArrayList<>();
    private long ultimoId = 0;

    void cadastra(RecursoPedido recurso) {
        recursos.add(recurso);
    }

    @Override
    public boolean recursoExiste(RecursoPedido recurso) {
        return recursos.contains(recurso);
    }

    @Override
    public List<ReservaNaAgenda> reservas() {
        return List.copyOf(reservas);
    }

    @Override
    public Optional<ReservaNaAgenda> busca(long id) {
        return reservas.stream().filter(r -> r.id() == id).findFirst();
    }

    @Override
    public ReservaNaAgenda grava(ReservaNaAgenda reserva) {
        long id = reserva.id() == 0 ? ++ultimoId : reserva.id();
        ReservaNaAgenda gravada = new ReservaNaAgenda(id, reserva.donoId(), reserva.estado(), reserva.inicio(),
                reserva.termino(), reserva.recursos(), reserva.criadaEm(), reserva.prazoAprovacaoEm());
        reservas.removeIf(r -> r.id() == id);
        reservas.add(gravada);
        return gravada;
    }
}
