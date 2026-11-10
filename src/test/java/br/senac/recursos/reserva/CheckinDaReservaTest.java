package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.CenarioDeReserva.DONO_A;
import static br.senac.recursos.reserva.CenarioDeReserva.assertRecusada;
import static br.senac.recursos.reserva.EstadoDaReserva.APROVADA;
import static br.senac.recursos.reserva.EstadoDaReserva.EM_USO;
import static br.senac.recursos.reserva.EstadoDaReserva.NAO_COMPARECEU;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Janela do check-in, {@code [início, fim)} (spec T-21, CA-01 e CA-16; TC-50 a TC-53). Reserva: terça, 10:00 às 11:00. */
class CheckinDaReservaTest {

    private ReservaNaAgenda reserva;

    private CenarioDeReserva cenarioEm(Instant agora) {
        CenarioDeReserva cenario = new CenarioDeReserva(agora);
        reserva = cenario.reserva(DONO_A, APROVADA, terca(10, 0), terca(11, 0), cenario.sala(102));
        return cenario;
    }

    @Test
    @DisplayName("TC-50: check-in às 09:59, antes do início, é recusado e continua APROVADA")
    void recusaCheckinAntesDoInicio() {
        CenarioDeReserva cenario = cenarioEm(terca(9, 59));

        assertRecusada(cenario.ciclo.checkin(DONO_A, reserva.id()), MotivoDeRecusa.FORA_DA_JANELA_DO_CHECKIN);
        assertThat(cenario.estadoDe(reserva)).isEqualTo(APROVADA);
    }

    @Test
    @DisplayName("TC-51: check-in às 10:00 em ponto vira EM_USO")
    void aceitaCheckinNoInicio() {
        CenarioDeReserva cenario = cenarioEm(terca(10, 0));

        cenario.assertMudouPara(cenario.ciclo.checkin(DONO_A, reserva.id()), reserva, EM_USO);
    }

    @Test
    @DisplayName("TC-52: check-in às 10:59 vira EM_USO")
    void aceitaCheckinUmMinutoAntesDoFim() {
        CenarioDeReserva cenario = cenarioEm(terca(10, 59));

        cenario.assertMudouPara(cenario.ciclo.checkin(DONO_A, reserva.id()), reserva, EM_USO);
    }

    @Test
    @DisplayName("TC-53: check-in às 11:00, no instante do fim, é recusado; a reserva segue para NAO_COMPARECEU")
    void recusaCheckinNoInstanteDoFim() {
        CenarioDeReserva cenario = cenarioEm(terca(11, 0));

        assertRecusada(cenario.ciclo.checkin(DONO_A, reserva.id()), MotivoDeRecusa.FORA_DA_JANELA_DO_CHECKIN);
        cenario.assertMudouPara(cenario.ciclo.muda(reserva.id(), NAO_COMPARECEU), reserva, NAO_COMPARECEU);
    }
}
