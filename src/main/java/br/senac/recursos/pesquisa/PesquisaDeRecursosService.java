package br.senac.recursos.pesquisa;

import br.senac.recursos.erro.EntradaInvalidaException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Pesquisa de recursos (spec T-43, RF-03). Os filtros se combinam por E. Capacidade e localização valem só para
 * salas e competência só para professores. A disponibilidade exclui recursos com reserva que ocupa o horário.
 */
@Service
public class PesquisaDeRecursosService {

    private static final Set<String> TIPOS = Set.of("SALA", "PROFESSOR", "MATERIAL");
    private static final String MOTIVO_INVALIDO = "valor inválido";

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public PesquisaDeRecursosService(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<RecursoPesquisado> pesquisa(String tipo, String capacidadeMinima, String localizacao,
            String competencia, String disponivelDe, String disponivelAte) {
        List<Map<String, String>> erros = new ArrayList<>();
        if (tipo != null && !TIPOS.contains(tipo)) {
            erros.add(erro("tipo", MOTIVO_INVALIDO));
        }
        Integer capacidade = inteiroPositivo(capacidadeMinima, erros);
        Instant de = instante("disponivelDe", disponivelDe, erros);
        Instant ate = instante("disponivelAte", disponivelAte, erros);
        if (erros.isEmpty()) {
            if ((de == null) != (ate == null)) {
                erros.add(erro(de == null ? "disponivelDe" : "disponivelAte", "deve ser informado junto com o outro limite"));
            } else if (de != null && !ate.isAfter(de)) {
                erros.add(erro("disponivelAte", "deve ser posterior ao início"));
            }
        }
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }

        StringBuilder sql = new StringBuilder(
                "SELECT r.id, r.tipo, r.nome, r.restrito, s.capacidade, s.localizacao, "
                        + "array_to_json(array(SELECT c.descricao FROM competencia c WHERE c.professor_id = r.id "
                        + "ORDER BY c.descricao)) AS competencias "
                        + "FROM recurso r LEFT JOIN sala s ON s.recurso_id = r.id WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (tipo != null) {
            sql.append(" AND r.tipo = ?");
            parametros.add(tipo);
        }
        if (capacidade != null) {
            sql.append(" AND r.tipo = 'SALA' AND s.capacidade >= ?");
            parametros.add(capacidade);
        }
        if (localizacao != null && !localizacao.isBlank()) {
            sql.append(" AND r.tipo = 'SALA' AND s.localizacao ILIKE ? ESCAPE '\\'");
            parametros.add(contem(localizacao));
        }
        if (competencia != null && !competencia.isBlank()) {
            sql.append(" AND r.tipo = 'PROFESSOR' AND EXISTS (SELECT 1 FROM competencia c "
                    + "WHERE c.professor_id = r.id AND c.descricao ILIKE ? ESCAPE '\\')");
            parametros.add(contem(competencia));
        }
        if (de != null) {
            sql.append(" AND NOT EXISTS (SELECT 1 FROM reserva_recurso rr WHERE rr.recurso_id = r.id AND rr.ocupa "
                    + "AND rr.periodo && tstzrange(?::timestamptz, ?::timestamptz, '[)'))");
            parametros.add(utc(de).toString());
            parametros.add(utc(ate).toString());
            // O recurso em manutenção também não está disponível (RN-04, T-35).
            sql.append(" AND NOT EXISTS (SELECT 1 FROM periodo_manutencao pm WHERE pm.recurso_id = r.id "
                    + "AND pm.periodo && tstzrange(?::timestamptz, ?::timestamptz, '[)'))");
            parametros.add(utc(de).toString());
            parametros.add(utc(ate).toString());
        }
        sql.append(" ORDER BY lower(r.nome), r.id");

        return jdbc.query(sql.toString(), (rs, n) -> new RecursoPesquisado(rs.getLong("id"), rs.getString("tipo"),
                rs.getString("nome"), rs.getBoolean("restrito"), (Integer) rs.getObject("capacidade"),
                rs.getString("localizacao"), competencias(rs.getString("tipo"), rs.getString("competencias"))),
                parametros.toArray());
    }

    private List<String> competencias(String tipo, String jsonDasCompetencias) {
        if (!"PROFESSOR".equals(tipo)) {
            return null;
        }
        try {
            return json.readValue(jsonDasCompetencias, new TypeReference<List<String>>() { });
        } catch (Exception erro) {
            throw new IllegalStateException("competências ilegíveis", erro);
        }
    }

    /** O texto entre %: contém, sem diferenciar maiúsculas; % e _ digitados valem como texto. */
    private static String contem(String texto) {
        return "%" + texto.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    private static Integer inteiroPositivo(String texto, List<Map<String, String>> erros) {
        if (texto == null) {
            return null;
        }
        try {
            int valor = Integer.parseInt(texto.trim());
            if (valor > 0) {
                return valor;
            }
        } catch (NumberFormatException erro) {
            // cai no erro abaixo
        }
        erros.add(erro("capacidadeMinima", "deve ser um inteiro maior que zero"));
        return null;
    }

    private static Instant instante(String campo, String texto, List<Map<String, String>> erros) {
        if (texto == null) {
            return null;
        }
        try {
            return Instant.parse(texto.trim());
        } catch (DateTimeParseException erro) {
            erros.add(erro(campo, MOTIVO_INVALIDO));
            return null;
        }
    }

    private static Map<String, String> erro(String campo, String motivo) {
        return Map.of("campo", campo, "motivo", motivo);
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
