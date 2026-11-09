package br.senac.recursos.reserva;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** A verificação da aplicação: consulta os vínculos que ocupam o recurso. A garantia final é o EXCLUDE do banco. */
@Component
public class SobreposicaoNoBanco implements VerificadorDeSobreposicao {

    private final JdbcTemplate jdbc;

    public SobreposicaoNoBanco(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean ocupado(RecursoPedido recurso, Instant inicio, Instant termino, Long reservaIgnorada) {
        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM reserva_recurso WHERE recurso_id = ? AND ocupa "
                        + "AND periodo && tstzrange(?::timestamptz, ?::timestamptz, '[)') "
                        + "AND (?::bigint IS NULL OR reserva_id <> ?)",
                Integer.class, recurso.id(), utc(inicio).toString(), utc(termino).toString(),
                reservaIgnorada, reservaIgnorada);
        return total != null && total > 0;
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
