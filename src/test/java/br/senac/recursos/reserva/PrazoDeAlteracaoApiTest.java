package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.HorariosDeTeste.quarta;
import static br.senac.recursos.reserva.HorariosDeTeste.segunda;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O PUT no limite do prazo de alteração (spec T-21, CA-13; TC-83). Reserva SOLICITADA de terça, prazo segunda 10:00. */
@TesteDeReservaApi
class PrazoDeAlteracaoApiTest extends BaseDaApiDeReserva {

    private static final Instant PRAZO = segunda(10, 0);

    @Test
    @DisplayName("TC-83: o PUT exatamente em prazo_aprovacao_em é aceito (200)")
    void alteraNoInstanteDoPrazo() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);
        relogio.defineAgora(PRAZO);

        ambiente.altera(ambiente.bearerDoSolicitante(), id,
                AmbienteDeReserva.corpo(quarta(10, 0), quarta(11, 0), sala)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-83: o PUT um segundo depois do prazo é recusado com 409 e a reserva não muda")
    void recusaAlterarUmSegundoDepoisDoPrazo() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);
        relogio.defineAgora(PRAZO.plusSeconds(1));

        ambiente.altera(ambiente.bearerDoSolicitante(), id,
                AmbienteDeReserva.corpo(quarta(10, 0), quarta(11, 0), sala))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta reserva não pode mais ser alterada"));
        assertThat(ambiente.inicioNoBanco(id)).isEqualTo(HorariosDeTeste.terca(10, 0));
    }
}
