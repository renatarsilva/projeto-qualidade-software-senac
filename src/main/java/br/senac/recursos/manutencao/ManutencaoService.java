package br.senac.recursos.manutencao;

import br.senac.recursos.erro.ConflitoDeNegocioException;
import br.senac.recursos.erro.EntradaInvalidaException;
import br.senac.recursos.erro.RecursoNaoEncontradoException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro, consulta e encerramento de períodos de manutenção (spec T-35). Registrar uma manutenção não cancela
 * reservas: só lista as afetadas (S-05).
 */
@Service
public class ManutencaoService {

    static final int LIMITE_DO_MOTIVO = 100;

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public ManutencaoService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional
    public ManutencaoResponse registra(long recursoId, ManutencaoRequest pedido) {
        // Bloqueio exclusivo da linha do recurso: serializa com as reservas do mesmo recurso (CEN-04).
        List<Long> existe = jdbc.queryForList("SELECT id FROM recurso WHERE id = ? FOR UPDATE", Long.class, recursoId);
        if (existe.isEmpty()) {
            throw new RecursoNaoEncontradoException();
        }
        valida(pedido);
        long id;
        try {
            id = jdbc.queryForObject("INSERT INTO periodo_manutencao (recurso_id, periodo, motivo) "
                    + "VALUES (?, tstzrange(?::timestamptz, ?::timestamptz, '[)'), ?) RETURNING id", Long.class,
                    recursoId, utc(pedido.inicio()).toString(), utc(pedido.termino()).toString(), pedido.motivo().trim());
        } catch (DataIntegrityViolationException erro) {
            throw new ConflitoDeNegocioException("O recurso já tem manutenção neste período");
        }
        List<Long> afetadas = jdbc.queryForList("SELECT DISTINCT r.id FROM reserva r "
                + "JOIN reserva_recurso rr ON rr.reserva_id = r.id WHERE rr.recurso_id = ? AND rr.ocupa "
                + "AND rr.periodo && tstzrange(?::timestamptz, ?::timestamptz, '[)') ORDER BY r.id", Long.class,
                recursoId, utc(pedido.inicio()).toString(), utc(pedido.termino()).toString());
        return new ManutencaoResponse(id, recursoId, pedido.inicio(), pedido.termino(), pedido.motivo().trim(), afetadas);
    }

    @Transactional(readOnly = true)
    public List<ManutencaoResponse> lista(long recursoId) {
        confereRecurso(recursoId);
        return jdbc.query("SELECT id, recurso_id, lower(periodo) AS inicio, upper(periodo) AS termino, motivo "
                + "FROM periodo_manutencao WHERE recurso_id = ? ORDER BY lower(periodo), id", (rs, n) ->
                new ManutencaoResponse(rs.getLong("id"), rs.getLong("recurso_id"),
                        rs.getObject("inicio", OffsetDateTime.class).toInstant(),
                        rs.getObject("termino", OffsetDateTime.class).toInstant(), rs.getString("motivo"), null),
                recursoId);
    }

    @Transactional
    public void encerra(long recursoId, long manutencaoId) {
        confereRecurso(recursoId);
        int apagadas = jdbc.update("DELETE FROM periodo_manutencao WHERE id = ? AND recurso_id = ?", manutencaoId,
                recursoId);
        if (apagadas == 0) {
            throw new RecursoNaoEncontradoException();
        }
    }

    private void confereRecurso(long recursoId) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM recurso WHERE id = ?", Integer.class, recursoId);
        if (total == null || total == 0) {
            throw new RecursoNaoEncontradoException();
        }
    }

    /** Término depois do início e depois de agora (Clock); motivo obrigatório, com até 100 caracteres. */
    private void valida(ManutencaoRequest pedido) {
        List<Map<String, String>> erros = new ArrayList<>();
        Instant agora = clock.instant();
        if (pedido.inicio() == null) {
            erros.add(erro("inicio", "deve ser informado"));
        }
        if (pedido.termino() == null) {
            erros.add(erro("termino", "deve ser informado"));
        } else if (pedido.inicio() != null && !pedido.termino().isAfter(pedido.inicio())) {
            erros.add(erro("termino", "deve ser posterior ao início"));
        } else if (!pedido.termino().isAfter(agora)) {
            erros.add(erro("termino", "deve ser posterior ao instante atual"));
        }
        if (pedido.motivo() == null || pedido.motivo().isBlank()) {
            erros.add(erro("motivo", "deve ser informado"));
        } else if (pedido.motivo().trim().length() > LIMITE_DO_MOTIVO) {
            erros.add(erro("motivo", "deve ter no máximo " + LIMITE_DO_MOTIVO + " caracteres"));
        }
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
    }

    private static Map<String, String> erro(String campo, String motivo) {
        return Map.of("campo", campo, "motivo", motivo);
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
