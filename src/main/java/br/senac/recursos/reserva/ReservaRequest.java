package br.senac.recursos.reserva;

import java.time.Instant;
import java.util.List;

/** Corpo do POST e do PUT. O cliente não informa dono, criada_em nem prazo: o que vier a mais é ignorado. */
public record ReservaRequest(Instant inicio, Instant termino, List<RecursoRequest> recursos) {

    public record RecursoRequest(TipoDeRecurso tipo, Long id) {
    }
}
