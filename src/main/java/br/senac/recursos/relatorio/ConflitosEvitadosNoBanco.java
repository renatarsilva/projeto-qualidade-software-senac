package br.senac.recursos.relatorio;

import br.senac.recursos.reserva.RecursoPedido;
import br.senac.recursos.reserva.RegistroDeConflitos;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava o conflito evitado em **transação própria**: a reserva recusada desfaz a sua transação, e o registro da recusa
 * precisa sobreviver a isso (spec T-41, CA-07).
 */
@Component
public class ConflitosEvitadosNoBanco implements RegistroDeConflitos {

    private final JdbcTemplate jdbc;

    public ConflitosEvitadosNoBanco(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registra(RecursoPedido recurso, Instant ocorridoEm) {
        jdbc.update("INSERT INTO conflito_evitado (recurso_id, ocorrido_em) VALUES (?, ?)", recurso.id(),
                OffsetDateTime.ofInstant(ocorridoEm, ZoneOffset.UTC));
    }
}
