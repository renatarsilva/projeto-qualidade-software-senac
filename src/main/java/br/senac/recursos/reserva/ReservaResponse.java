package br.senac.recursos.reserva;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** A reserva na resposta. O {@code aviso} só aparece quando uma reserva aprovada volta a ser solicitada. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservaResponse(long id, EstadoDaReserva estado, Instant inicio, Instant termino, Instant criadaEm,
        Instant prazoAprovacaoEm, List<RecursoPedido> recursos, String aviso) {

    static ReservaResponse de(DecisaoDaReserva decisao) {
        ReservaNaAgenda r = decisao.reserva();
        return new ReservaResponse(r.id(), r.estado(), r.inicio(), r.termino(), r.criadaEm(), r.prazoAprovacaoEm(),
                r.recursos(), decisao.aviso());
    }
}
