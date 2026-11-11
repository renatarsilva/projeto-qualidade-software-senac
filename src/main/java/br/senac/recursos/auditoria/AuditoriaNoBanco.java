package br.senac.recursos.auditoria;

import br.senac.recursos.reserva.EstadoDaReserva;
import java.time.Clock;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** A auditoria no PostgreSQL. Esqueleto da T-40: não grava nada até a implementação. */
@Repository
public class AuditoriaNoBanco implements AuditoriaDeReservas {

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public AuditoriaNoBanco(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public void registra(long reservaId, Long usuarioId, EstadoDaReserva anterior, EstadoDaReserva novo,
            String motivo) {
        // T-40 (green)
    }

    @Override
    public List<RegistroDeAuditoria> historico(long reservaId) {
        return List.of();
    }
}
