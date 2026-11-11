package br.senac.recursos.manutencao;

import br.senac.recursos.reserva.RecursoPedido;
import br.senac.recursos.reserva.VerificadorDeManutencao;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Manutenção no PostgreSQL. A verificação da reserva primeiro **compartilha o bloqueio da linha do recurso**: a
 * manutenção pede o bloqueio exclusivo da mesma linha, então uma não passa na frente da outra (CEN-04, spec T-35).
 * Precisa rodar dentro da transação da reserva.
 */
@Component
public class ManutencaoNoBanco implements VerificadorDeManutencao {

    private final JdbcTemplate jdbc;

    public ManutencaoNoBanco(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean emManutencao(RecursoPedido recurso, Instant inicio, Instant termino) {
        jdbc.query("SELECT id FROM recurso WHERE id = ? FOR SHARE", rs -> { }, recurso.id());
        Integer total = jdbc.queryForObject("SELECT count(*) FROM periodo_manutencao WHERE recurso_id = ? "
                + "AND periodo && tstzrange(?::timestamptz, ?::timestamptz, '[)')", Integer.class, recurso.id(),
                utc(inicio).toString(), utc(termino).toString());
        return total != null && total > 0;
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
