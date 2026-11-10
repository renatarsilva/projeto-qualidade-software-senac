package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.CenarioDeReserva.DONO_A;
import static br.senac.recursos.reserva.CenarioDeReserva.assertRecusada;
import static br.senac.recursos.reserva.EstadoDaReserva.APROVADA;
import static br.senac.recursos.reserva.EstadoDaReserva.CANCELADA;
import static br.senac.recursos.reserva.EstadoDaReserva.EM_USO;
import static br.senac.recursos.reserva.EstadoDaReserva.NAO_COMPARECEU;
import static br.senac.recursos.reserva.EstadoDaReserva.REJEITADA;
import static br.senac.recursos.reserva.EstadoDaReserva.SOLICITADA;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** Caminhos alternativos, estados finais e estado inexistente (spec T-21, CA-03 a CA-05; TC-08 a TC-17). */
class CaminhosAlternativosTest {

    @Test
    @DisplayName("TC-08 (RN-06-T1): SOLICITADA rejeitada pelo Responsável vira REJEITADA")
    void rejeitaSolicitada() {
        CenarioDeReserva cenario = new CenarioDeReserva();
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, SOLICITADA, terca(10, 0), terca(11, 0), cenario.sala(102));

        cenario.assertMudouPara(cenario.ciclo.muda(reserva.id(), REJEITADA), reserva, REJEITADA);
    }

    @Test
    @DisplayName("TC-09 (RN-06-T2): SOLICITADA cancelada pelo dono vira CANCELADA")
    void donoCancelaSolicitada() {
        CenarioDeReserva cenario = new CenarioDeReserva();
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, SOLICITADA, terca(10, 0), terca(11, 0), cenario.sala(102));

        cenario.assertMudouPara(cenario.ciclo.cancela(DONO_A, reserva.id()), reserva, CANCELADA);
    }

    @Test
    @DisplayName("TC-10 (RN-06-T3): APROVADA cancelada pelo dono antes do início vira CANCELADA")
    void donoCancelaAprovadaAntesDoInicio() {
        CenarioDeReserva cenario = new CenarioDeReserva();
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, APROVADA, terca(10, 0), terca(11, 0), cenario.sala(102));

        cenario.assertMudouPara(cenario.ciclo.cancela(DONO_A, reserva.id()), reserva, CANCELADA);
    }

    @Test
    @DisplayName("TC-11 (RN-06-T4): APROVADA sem check-in às 11:00, no fim, vira NAO_COMPARECEU e o horário fica livre")
    void semCheckinNoFimViraNaoCompareceu() {
        CenarioDeReserva cenario = new CenarioDeReserva(terca(11, 0));
        RecursoPedido sala = cenario.sala(102);
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, APROVADA, terca(10, 0), terca(11, 0), sala);

        cenario.assertMudouPara(cenario.ciclo.muda(reserva.id(), NAO_COMPARECEU), reserva, NAO_COMPARECEU);
        assertThat(cenario.ocupado(sala, terca(10, 0), terca(11, 0))).as("horário ocupado").isFalse();
    }

    @Test
    @DisplayName("TC-12 (RN-06-T11): NAO_COMPARECEU às 10:35, antes do fim e sem check-in, é recusado")
    void recusaNaoCompareceuAntesDoFim() {
        CenarioDeReserva cenario = new CenarioDeReserva(terca(10, 35));
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, APROVADA, terca(10, 0), terca(11, 0), cenario.sala(102));

        assertRecusada(cenario.ciclo.muda(reserva.id(), NAO_COMPARECEU), MotivoDeRecusa.TRANSICAO_PROIBIDA);
        assertThat(cenario.estadoDe(reserva)).isEqualTo(APROVADA);
    }

    @Test
    @DisplayName("TC-13 (RN-06-T9): SOLICITADA sem aprovação às 09:01, com prazo às 09:00, é cancelada pelo sistema")
    void sistemaCancelaPorPrazoVencido() {
        CenarioDeReserva cenario = new CenarioDeReserva(terca(9, 1));
        ReservaNaAgenda reserva = cenario.reservaComPrazo(DONO_A, SOLICITADA, terca(10, 0), terca(11, 0),
                terca(9, 0), cenario.sala(102));

        cenario.assertMudouPara(cenario.ciclo.muda(reserva.id(), CANCELADA), reserva, CANCELADA);
    }

    @Test
    @DisplayName("TC-14 (RN-06-T5): EM_USO para CANCELADA por transição direta é recusada")
    void recusaEmUsoParaCancelada() {
        CenarioDeReserva cenario = new CenarioDeReserva(terca(10, 30));
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, EM_USO, terca(10, 0), terca(11, 0), cenario.sala(102));

        assertRecusada(cenario.ciclo.muda(reserva.id(), CANCELADA), MotivoDeRecusa.TRANSICAO_PROIBIDA);
        assertThat(cenario.estadoDe(reserva)).isEqualTo(EM_USO);
    }

    @ParameterizedTest(name = "TC-15: EM_USO para {0} é recusada")
    @EnumSource(value = EstadoDaReserva.class, names = {"REJEITADA", "NAO_COMPARECEU"})
    @DisplayName("TC-15 (RN-06-T6): EM_USO para REJEITADA ou NAO_COMPARECEU é recusada")
    void recusaEmUsoParaRejeitadaOuNaoCompareceu(EstadoDaReserva destino) {
        CenarioDeReserva cenario = new CenarioDeReserva(terca(11, 0));
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, EM_USO, terca(10, 0), terca(11, 0), cenario.sala(102));

        assertRecusada(cenario.ciclo.muda(reserva.id(), destino), MotivoDeRecusa.TRANSICAO_PROIBIDA);
        assertThat(cenario.estadoDe(reserva)).isEqualTo(EM_USO);
    }

    static Stream<Arguments> saidasDeEstadosFinais() {
        Stream.Builder<Arguments> casos = Stream.builder();
        for (EstadoDaReserva final_ : new EstadoDaReserva[] {EstadoDaReserva.CONCLUIDA, REJEITADA, CANCELADA,
                NAO_COMPARECEU}) {
            for (EstadoDaReserva destino : EstadoDaReserva.values()) {
                casos.add(Arguments.of(final_, destino));
            }
        }
        return casos.build();
    }

    @ParameterizedTest(name = "TC-16: de {0} para {1} é recusada")
    @MethodSource("saidasDeEstadosFinais")
    @DisplayName("TC-16 (RN-06-T7): estados finais não têm saída")
    void estadosFinaisNaoTemSaida(EstadoDaReserva final_, EstadoDaReserva destino) {
        CenarioDeReserva cenario = new CenarioDeReserva(terca(12, 0));
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, final_, terca(10, 0), terca(11, 0), cenario.sala(102));

        assertRecusada(cenario.ciclo.muda(reserva.id(), destino), MotivoDeRecusa.TRANSICAO_PROIBIDA);
        assertThat(cenario.estadoDe(reserva)).isEqualTo(final_);
    }

    @Test
    @DisplayName("TC-17 (RN-06-T8): mudar para um estado que não existe (\"ABC\") é recusado")
    void recusaEstadoInexistente() {
        CenarioDeReserva cenario = new CenarioDeReserva();
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, SOLICITADA, terca(10, 0), terca(11, 0), cenario.sala(102));

        assertRecusada(cenario.ciclo.muda(reserva.id(), "ABC"), MotivoDeRecusa.ESTADO_INEXISTENTE);
        assertThat(cenario.estadoDe(reserva)).isEqualTo(SOLICITADA);
    }
}
