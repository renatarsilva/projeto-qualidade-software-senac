package br.senac.recursos.reserva;

import br.senac.recursos.auditoria.AuditoriaDeReservas;
import br.senac.recursos.auditoria.RegistroDeAuditoria;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/** Auditoria dos testes unitários: só guarda os registros, com o instante do Clock. */
final class AuditoriaEmMemoria implements AuditoriaDeReservas {

    private final Clock clock;
    private final List<RegistroDeAuditoria> registros = new ArrayList<>();

    AuditoriaEmMemoria(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void registra(long reservaId, Long usuarioId, EstadoDaReserva anterior, EstadoDaReserva novo,
            String motivo) {
        registros.add(new RegistroDeAuditoria(registros.size() + 1L, reservaId, usuarioId, anterior, novo,
                clock.instant(), motivo));
    }

    @Override
    public List<RegistroDeAuditoria> historico(long reservaId) {
        return registros.stream().filter(r -> r.reservaId() == reservaId).toList();
    }

    List<RegistroDeAuditoria> todos() {
        return List.copyOf(registros);
    }
}
