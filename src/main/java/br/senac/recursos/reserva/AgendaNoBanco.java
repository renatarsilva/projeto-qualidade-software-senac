package br.senac.recursos.reserva;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** A agenda no PostgreSQL: grava a reserva e os vínculos com os recursos, copiando período e ocupação (D-02). */
@Repository
public class AgendaNoBanco implements AgendaDeReservas {

    private static final String SELECT_RESERVA =
            "SELECT id, solicitante_id, estado, inicio, fim, criada_em, prazo_aprovacao_em FROM reserva";

    private final JdbcTemplate jdbc;

    public AgendaNoBanco(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean recursoExiste(RecursoPedido recurso) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM recurso WHERE id = ? AND tipo = ?",
                Integer.class, recurso.id(), recurso.tipo().name());
        return total != null && total > 0;
    }

    @Override
    public List<ReservaNaAgenda> reservas() {
        return jdbc.query(SELECT_RESERVA + " ORDER BY id", (rs, n) -> reserva(rs));
    }

    @Override
    public Optional<ReservaNaAgenda> busca(long id) {
        return jdbc.query(SELECT_RESERVA + " WHERE id = ?", (rs, n) -> reserva(rs), id).stream().findFirst();
    }

    @Override
    public ReservaNaAgenda grava(ReservaNaAgenda reserva) {
        long id = reserva.id();
        if (id == 0) {
            id = jdbc.queryForObject(
                    "INSERT INTO reserva (solicitante_id, inicio, fim, estado, criada_em, prazo_aprovacao_em) "
                            + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
                    Long.class, reserva.donoId(), utc(reserva.inicio()), utc(reserva.termino()),
                    reserva.estado().name(), utc(reserva.criadaEm()), utc(reserva.prazoAprovacaoEm()));
        } else {
            jdbc.update("UPDATE reserva SET inicio = ?, fim = ?, estado = ?, prazo_aprovacao_em = ? WHERE id = ?",
                    utc(reserva.inicio()), utc(reserva.termino()), reserva.estado().name(),
                    utc(reserva.prazoAprovacaoEm()), id);
            jdbc.update("DELETE FROM reserva_recurso WHERE reserva_id = ?", id);
        }
        // Sempre na mesma ordem (por id): pedidos com os mesmos recursos em ordem invertida não travam um ao outro (ADR-0010).
        List<RecursoPedido> emOrdem = reserva.recursos().stream()
                .sorted(java.util.Comparator.comparingLong(RecursoPedido::id)).toList();
        for (RecursoPedido recurso : emOrdem) {
            jdbc.update("INSERT INTO reserva_recurso (reserva_id, recurso_id, periodo, ocupa) "
                    + "VALUES (?, ?, tstzrange(?::timestamptz, ?::timestamptz, '[)'), ?)",
                    id, recurso.id(), utc(reserva.inicio()).toString(), utc(reserva.termino()).toString(),
                    reserva.estado().ocupaHorario());
        }
        return new ReservaNaAgenda(id, reserva.donoId(), reserva.estado(), reserva.inicio(), reserva.termino(),
                reserva.recursos(), reserva.criadaEm(), reserva.prazoAprovacaoEm());
    }

    private ReservaNaAgenda reserva(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        List<RecursoPedido> recursos = new ArrayList<>(jdbc.query(
                "SELECT r.tipo, rr.recurso_id FROM reserva_recurso rr JOIN recurso r ON r.id = rr.recurso_id "
                        + "WHERE rr.reserva_id = ? ORDER BY rr.id",
                (linha, n) -> new RecursoPedido(TipoDeRecurso.valueOf(linha.getString("tipo")),
                        linha.getLong("recurso_id")), id));
        return new ReservaNaAgenda(id, rs.getLong("solicitante_id"), EstadoDaReserva.valueOf(rs.getString("estado")),
                instante(rs, "inicio"), instante(rs, "fim"), recursos, instante(rs, "criada_em"),
                instante(rs, "prazo_aprovacao_em"));
    }

    private static Instant instante(ResultSet rs, String coluna) throws SQLException {
        return rs.getObject(coluna, OffsetDateTime.class).toInstant();
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
