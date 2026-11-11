package br.senac.recursos.auditoria;

import br.senac.recursos.reserva.EstadoDaReserva;
import java.util.List;

/** Onde a mudança de estado deixa o registro. A gravação acontece na mesma transação da mudança (spec T-40). */
public interface AuditoriaDeReservas {

    /** Grava um registro; o instante vem do {@code Clock}. */
    void registra(long reservaId, Long usuarioId, EstadoDaReserva anterior, EstadoDaReserva novo, String motivo);

    /** O histórico da reserva, do mais antigo ao mais novo. */
    List<RegistroDeAuditoria> historico(long reservaId);
}
