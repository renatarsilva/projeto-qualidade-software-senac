package br.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Contrato do POST /api/reservas/{id}/checkin (spec T-21, CA-16; TC-62 a TC-66). Reserva: terça, 10:00 às 11:00. */
@TesteDeReservaApi
class CheckinApiTest extends BaseDaApiDeReserva {

    private static final String MENSAGEM = "O check-in só pode ser feito durante o horário reservado";

    @Test
    @DisplayName("TC-62: o dono faz o check-in às 10:30 numa APROVADA e recebe 200 com a reserva EM_USO")
    void donoFazCheckin() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.APROVADA);
        relogio.defineAgora(HorariosDeTeste.terca(10, 30));

        ambiente.checkin(ambiente.bearerDoSolicitante(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EM_USO"));
        assertThat(ambiente.estadoNoBanco(id)).isEqualTo("EM_USO");
    }

    @Test
    @DisplayName("TC-63: quem não é o dono recebe 403 e a reserva continua APROVADA")
    void naoDonoNaoFazCheckin() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.APROVADA);
        relogio.defineAgora(HorariosDeTeste.terca(10, 30));

        ambiente.checkin(ambiente.bearerDoOutro(), id).andExpect(status().isForbidden());
        assertThat(ambiente.estadoNoBanco(id)).isEqualTo("APROVADA");
    }

    @ParameterizedTest(name = "TC-64: check-in em {0} devolve 409")
    @ValueSource(strings = {"2026-11-10T09:59:00Z", "2026-11-10T11:00:00Z"})
    @DisplayName("TC-64: check-in às 09:59 e às 11:00 é recusado com 409 e a mensagem do horário reservado")
    void checkinForaDaJanela(String instante) throws Exception {
        long id = reservaDoDono(EstadoDaReserva.APROVADA);
        relogio.defineAgora(Instant.parse(instante));

        ambiente.checkin(ambiente.bearerDoSolicitante(), id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(MENSAGEM));
        assertThat(ambiente.estadoNoBanco(id)).isEqualTo("APROVADA");
    }

    @Test
    @DisplayName("TC-65: check-in numa reserva SOLICITADA devolve 409 e o estado não muda")
    void checkinDeSolicitada() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);
        relogio.defineAgora(HorariosDeTeste.terca(10, 30));

        ambiente.checkin(ambiente.bearerDoSolicitante(), id).andExpect(status().isConflict());
        assertThat(ambiente.estadoNoBanco(id)).isEqualTo("SOLICITADA");
    }

    @Test
    @DisplayName("TC-66: check-in numa reserva que não existe devolve 404")
    void checkinDeReservaInexistente() throws Exception {
        ambiente.checkin(ambiente.bearerDoSolicitante(), 999999)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Recurso não encontrado"));
    }
}
