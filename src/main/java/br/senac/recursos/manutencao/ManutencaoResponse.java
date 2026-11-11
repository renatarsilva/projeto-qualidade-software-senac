package br.senac.recursos.manutencao;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** Um período de manutenção. {@code reservasAfetadas} só vem no registro (S-05). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ManutencaoResponse(long id, long recursoId, Instant inicio, Instant termino, String motivo,
        List<Long> reservasAfetadas) {
}
