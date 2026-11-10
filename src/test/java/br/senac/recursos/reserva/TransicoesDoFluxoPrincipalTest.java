package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.CenarioDeReserva.DONO_A;
import static br.senac.recursos.reserva.CenarioDeReserva.assertRecusada;
import static br.senac.recursos.reserva.EstadoDaReserva.APROVADA;
import static br.senac.recursos.reserva.EstadoDaReserva.CONCLUIDA;
import static br.senac.recursos.reserva.EstadoDaReserva.EM_USO;
import static br.senac.recursos.reserva.EstadoDaReserva.SOLICITADA;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Fluxo principal: avançar um passo por vez e não pular nem voltar (spec T-21, CA-01 e CA-02; TC-01 a TC-07). */
class TransicoesDoFluxoPrincipalTest {

    @Test
    @DisplayName("TC-01 (RN-05-T1): SOLICITADA aprovada pelo Responsável vira APROVADA")
    void aprovaSolicitada() {
        CenarioDeReserva cenario = new CenarioDeReserva();
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, SOLICITADA, terca(10, 0), terca(11, 0), cenario.sala(102));

        cenario.assertMudouPara(cenario.ciclo.muda(reserva.id(), APROVADA), reserva, APROVADA);
    }

    @Test
    @DisplayName("TC-02 (RN-05-T2): APROVADA com check-in do dono às 10:30 vira EM_USO")
    void checkinLevaAEmUso() {
        CenarioDeReserva cenario = new CenarioDeReserva(terca(10, 30));
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, APROVADA, terca(10, 0), terca(11, 0), cenario.sala(102));

        cenario.assertMudouPara(cenario.ciclo.checkin(DONO_A, reserva.id()), reserva, EM_USO);
    }

    @Test
    @DisplayName("TC-03 (RN-05-T3): EM_USO encerrada pelo sistema às 11:00, no fim, vira CONCLUIDA")
    void sistemaConcluiNoFim() {
        CenarioDeReserva cenario = new CenarioDeReserva(terca(11, 0));
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, EM_USO, terca(10, 0), terca(11, 0), cenario.sala(102));

        cenario.assertMudouPara(cenario.ciclo.muda(reserva.id(), CONCLUIDA), reserva, CONCLUIDA);
    }

    @ParameterizedTest(name = "{0} direto para {1} é recusada")
    @CsvSource({"SOLICITADA, EM_USO", "SOLICITADA, CONCLUIDA", "APROVADA, CONCLUIDA"})
    @DisplayName("TC-04 a TC-06 (RN-05-T4 a T6): pular um estado é recusado e o estado não muda")
    void recusaPularEstado(EstadoDaReserva origem, EstadoDaReserva destino) {
        CenarioDeReserva cenario = new CenarioDeReserva();
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, origem, terca(10, 0), terca(11, 0), cenario.sala(102));

        assertRecusada(cenario.ciclo.muda(reserva.id(), destino), MotivoDeRecusa.TRANSICAO_PROIBIDA);
        assertThat(cenario.estadoDe(reserva)).isEqualTo(origem);
    }

    @ParameterizedTest(name = "TC-07: retrocesso {0} para {1} é recusado")
    @CsvSource({"APROVADA, SOLICITADA", "EM_USO, APROVADA", "CONCLUIDA, EM_USO"})
    @DisplayName("TC-07 (RN-05-T7): voltar no fluxo é recusado e o estado não muda")
    void recusaRetrocesso(EstadoDaReserva origem, EstadoDaReserva destino) {
        CenarioDeReserva cenario = new CenarioDeReserva();
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, origem, terca(10, 0), terca(11, 0), cenario.sala(102));

        assertRecusada(cenario.ciclo.muda(reserva.id(), destino), MotivoDeRecusa.TRANSICAO_PROIBIDA);
        assertThat(cenario.estadoDe(reserva)).isEqualTo(origem);
    }
}
