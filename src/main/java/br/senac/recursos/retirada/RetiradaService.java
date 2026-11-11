package br.senac.recursos.retirada;

import br.senac.recursos.erro.ConflitoDeNegocioException;
import br.senac.recursos.erro.EntradaInvalidaException;
import br.senac.recursos.erro.RecursoNaoEncontradoException;
import br.senac.recursos.reserva.AgendaDeReservas;
import br.senac.recursos.reserva.EstadoDaReserva;
import br.senac.recursos.reserva.RecursoPedido;
import br.senac.recursos.reserva.ReservaNaAgenda;
import br.senac.recursos.reserva.TipoDeRecurso;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retirada e devolução de materiais (spec T-42, RF-09). O prazo de devolução é o término da reserva. A ordem dos
 * erros é 404, 400 e 409.
 */
@Service
public class RetiradaService {

    private final JdbcTemplate jdbc;
    private final AgendaDeReservas agenda;
    private final Clock clock;

    public RetiradaService(JdbcTemplate jdbc, AgendaDeReservas agenda, Clock clock) {
        this.jdbc = jdbc;
        this.agenda = agenda;
        this.clock = clock;
    }

    @Transactional
    public RetiradaResponse retira(long usuarioId, long reservaId, Long materialId) {
        ReservaNaAgenda reserva = reserva(reservaId);
        if (materialId == null) {
            throw new EntradaInvalidaException(List.of(Map.of("campo", "materialId", "motivo", "deve ser informado")));
        }
        if (!agenda.recursoExiste(new RecursoPedido(TipoDeRecurso.MATERIAL, materialId))) {
            throw new RecursoNaoEncontradoException();
        }
        Instant agora = clock.instant();
        if (reserva.estado() != EstadoDaReserva.APROVADA && reserva.estado() != EstadoDaReserva.EM_USO) {
            throw new ConflitoDeNegocioException("Só se retira material de uma reserva aprovada");
        }
        if (!reserva.recursos().contains(new RecursoPedido(TipoDeRecurso.MATERIAL, materialId))) {
            throw new ConflitoDeNegocioException("O material não faz parte desta reserva");
        }
        if (!agora.isBefore(reserva.termino())) {
            throw new ConflitoDeNegocioException("A reserva já terminou");
        }
        try {
            long id = jdbc.queryForObject("INSERT INTO retirada (reserva_id, material_id, registrada_por, retirada_em) "
                    + "VALUES (?, ?, ?, ?) RETURNING id", Long.class, reservaId, materialId, usuarioId, utc(agora));
            return busca(id, reserva);
        } catch (DataIntegrityViolationException erro) {
            // Índice da retirada aberta: o material já saiu e não voltou.
            throw new ConflitoDeNegocioException("Este material já foi retirado");
        }
    }

    @Transactional
    public RetiradaResponse devolve(long reservaId, long retiradaId) {
        ReservaNaAgenda reserva = reserva(reservaId);
        RetiradaResponse atual = jdbc.query(SELECT + " WHERE id = ? AND reserva_id = ?", mapeador(reserva), retiradaId,
                reservaId).stream().findFirst().orElseThrow(RecursoNaoEncontradoException::new);
        if (atual.devolvidaEm() != null) {
            throw new ConflitoDeNegocioException("Esta devolução já foi registrada");
        }
        int atualizadas = jdbc.update("UPDATE retirada SET devolvida_em = ? WHERE id = ? AND devolvida_em IS NULL",
                utc(clock.instant()), retiradaId);
        if (atualizadas == 0) {
            throw new ConflitoDeNegocioException("Esta devolução já foi registrada");
        }
        return busca(retiradaId, reserva);
    }

    @Transactional(readOnly = true)
    public List<RetiradaResponse> lista(long reservaId) {
        ReservaNaAgenda reserva = reserva(reservaId);
        return jdbc.query(SELECT + " WHERE reserva_id = ? ORDER BY id", mapeador(reserva), reservaId);
    }

    private static final String SELECT =
            "SELECT id, reserva_id, material_id, registrada_por, retirada_em, devolvida_em FROM retirada";

    private ReservaNaAgenda reserva(long reservaId) {
        return agenda.busca(reservaId).orElseThrow(RecursoNaoEncontradoException::new);
    }

    private RetiradaResponse busca(long id, ReservaNaAgenda reserva) {
        return jdbc.query(SELECT + " WHERE id = ?", mapeador(reserva), id).get(0);
    }

    /** Atrasada: a devolução depois do término, ou, sem devolução, o instante atual depois do término. */
    private RowMapper<RetiradaResponse> mapeador(ReservaNaAgenda reserva) {
        return (rs, n) -> {
            Instant retiradaEm = rs.getObject("retirada_em", OffsetDateTime.class).toInstant();
            OffsetDateTime devolvida = rs.getObject("devolvida_em", OffsetDateTime.class);
            Instant devolvidaEm = devolvida == null ? null : devolvida.toInstant();
            Instant referencia = devolvidaEm != null ? devolvidaEm : clock.instant();
            return new RetiradaResponse(rs.getLong("id"), rs.getLong("reserva_id"), rs.getLong("material_id"),
                    rs.getLong("registrada_por"), retiradaEm, devolvidaEm, referencia.isAfter(reserva.termino()));
        };
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
