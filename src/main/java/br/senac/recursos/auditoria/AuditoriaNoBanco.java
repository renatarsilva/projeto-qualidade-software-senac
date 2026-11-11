package br.senac.recursos.auditoria;

import br.senac.recursos.reserva.EstadoDaReserva;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** A auditoria no PostgreSQL. Grava na transação de quem chama; a tabela só recebe inserções (gatilho do banco). */
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
        jdbc.update("INSERT INTO auditoria (reserva_id, usuario_id, estado_anterior, estado_novo, ocorrida_em, motivo) "
                + "VALUES (?, ?, ?, ?, ?, ?)", reservaId, usuarioId, anterior == null ? null : anterior.name(),
                novo.name(), OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC), motivo);
    }

    @Override
    public List<RegistroDeAuditoria> historico(long reservaId) {
        return jdbc.query("SELECT id, reserva_id, usuario_id, estado_anterior, estado_novo, ocorrida_em, motivo "
                + "FROM auditoria WHERE reserva_id = ? ORDER BY id", (rs, n) -> new RegistroDeAuditoria(
                        rs.getLong("id"), rs.getLong("reserva_id"), (Long) rs.getObject("usuario_id"),
                        estado(rs.getString("estado_anterior")), estado(rs.getString("estado_novo")),
                        rs.getObject("ocorrida_em", OffsetDateTime.class).toInstant(), rs.getString("motivo")),
                reservaId);
    }

    private static EstadoDaReserva estado(String texto) {
        return texto == null ? null : EstadoDaReserva.valueOf(texto);
    }
}
