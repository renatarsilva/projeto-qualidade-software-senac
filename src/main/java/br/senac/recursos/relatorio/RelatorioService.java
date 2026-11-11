package br.senac.recursos.relatorio;

import br.senac.recursos.erro.EntradaInvalidaException;
import br.senac.recursos.relatorio.RelatorioDeUtilizacao.CargaDoProfessor;
import br.senac.recursos.relatorio.RelatorioDeUtilizacao.HorasDoRecurso;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relatório de utilização (spec T-41). Contam as reservas que não foram canceladas, rejeitadas nem marcadas como não
 * comparecimento; a reserva que cruza a borda do período conta só pelo trecho de dentro.
 */
@Service
public class RelatorioService {

    private static final String HORAS_POR_RECURSO =
            "SELECT r.id, r.tipo, r.nome, round((sum(EXTRACT(EPOCH FROM (LEAST(upper(rr.periodo), ?::timestamptz) "
                    + "- GREATEST(lower(rr.periodo), ?::timestamptz)))) / 3600.0)::numeric, 2) AS horas "
                    + "FROM reserva_recurso rr JOIN reserva rv ON rv.id = rr.reserva_id "
                    + "JOIN recurso r ON r.id = rr.recurso_id "
                    + "WHERE rv.estado IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA') "
                    + "AND rr.periodo && tstzrange(?::timestamptz, ?::timestamptz, '[)') "
                    + "GROUP BY r.id, r.tipo, r.nome ORDER BY lower(r.nome), r.id";

    private final JdbcTemplate jdbc;

    public RelatorioService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public RelatorioDeUtilizacao utilizacao(String inicioTexto, String fimTexto) {
        List<Map<String, String>> erros = new ArrayList<>();
        Instant inicio = instante("inicio", inicioTexto, erros);
        Instant fim = instante("fim", fimTexto, erros);
        if (erros.isEmpty() && !fim.isAfter(inicio)) {
            erros.add(Map.of("campo", "fim", "motivo", "deve ser posterior ao início"));
        }
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        String de = utc(inicio).toString();
        String ate = utc(fim).toString();

        List<HorasDoRecurso> recursos = jdbc.query(HORAS_POR_RECURSO, (rs, n) -> new HorasDoRecurso(rs.getLong("id"),
                rs.getString("tipo"), rs.getString("nome"), rs.getBigDecimal("horas").doubleValue()), ate, de, de, ate);
        List<CargaDoProfessor> professores = recursos.stream().filter(r -> "PROFESSOR".equals(r.tipo()))
                .map(r -> new CargaDoProfessor(r.recursoId(), r.nome(), r.horasReservadas())).toList();
        Integer conflitos = jdbc.queryForObject("SELECT count(*) FROM conflito_evitado "
                + "WHERE ocorrido_em >= ?::timestamptz AND ocorrido_em < ?::timestamptz", Integer.class, de, ate);
        return new RelatorioDeUtilizacao(inicio, fim, recursos, professores, conflitos == null ? 0 : conflitos);
    }

    private static Instant instante(String campo, String texto, List<Map<String, String>> erros) {
        if (texto == null || texto.isBlank()) {
            erros.add(Map.of("campo", campo, "motivo", "deve ser informado"));
            return null;
        }
        try {
            return Instant.parse(texto.trim());
        } catch (DateTimeParseException erro) {
            erros.add(Map.of("campo", campo, "motivo", "valor inválido"));
            return null;
        }
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
