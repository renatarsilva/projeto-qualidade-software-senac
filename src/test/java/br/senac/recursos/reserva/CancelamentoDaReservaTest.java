package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.CenarioDeReserva.DONO_A;
import static br.senac.recursos.reserva.CenarioDeReserva.DONO_B;
import static br.senac.recursos.reserva.CenarioDeReserva.assertRecusada;
import static br.senac.recursos.reserva.EstadoDaReserva.APROVADA;
import static br.senac.recursos.reserva.EstadoDaReserva.CANCELADA;
import static br.senac.recursos.reserva.EstadoDaReserva.CONCLUIDA;
import static br.senac.recursos.reserva.EstadoDaReserva.EM_USO;
import static br.senac.recursos.reserva.EstadoDaReserva.SOLICITADA;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Cancelamento pelo dono e quem pode cancelar ou alterar (spec T-21, CA-07 a CA-10, CA-12;
 * TC-19, TC-21 a TC-24, TC-26 a TC-32, TC-38 e TC-39). Reserva padrão: terça, 10:00 às 11:00.
 */
class CancelamentoDaReservaTest {

    @Test
    @DisplayName("TC-19 (RN-13-T1): cancelar às 08:00, antes do início, vira CANCELADA e libera o horário")
    void cancelarAntesDoInicioLiberaOHorario() {
        CenarioDeReserva cenario = new CenarioDeReserva();
        RecursoPedido sala = cenario.sala(102);
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, APROVADA, terca(10, 0), terca(11, 0), sala);

        cenario.assertMudouPara(cenario.ciclo.cancela(DONO_A, reserva.id()), reserva, CANCELADA);
        assertThat(cenario.ocupado(sala, terca(10, 0), terca(11, 0))).as("horário ocupado").isFalse();
    }

    @Test
    @DisplayName("TC-21 (RN-13-T3): cancelar uma SOLICITADA antes do início vira CANCELADA e libera o horário")
    void cancelarSolicitadaLiberaOHorario() {
        CenarioDeReserva cenario = new CenarioDeReserva();
        RecursoPedido sala = cenario.sala(102);
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, SOLICITADA, terca(10, 0), terca(11, 0), sala);

        cenario.assertMudouPara(cenario.ciclo.cancela(DONO_A, reserva.id()), reserva, CANCELADA);
        assertThat(cenario.ocupado(sala, terca(10, 0), terca(11, 0))).as("horário ocupado").isFalse();
    }

    @Test
    @DisplayName("TC-22 (RN-13-T4): cancelar às 09:59 vira CANCELADA")
    void cancelarUmMinutoAntes() {
        CenarioDeReserva cenario = new CenarioDeReserva(terca(9, 59));
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, APROVADA, terca(10, 0), terca(11, 0), cenario.sala(102));

        cenario.assertMudouPara(cenario.ciclo.cancela(DONO_A, reserva.id()), reserva, CANCELADA);
    }

    @Test
    @DisplayName("TC-23 (RN-13-T5): cancelar às 10:00 em ponto vira CANCELADA e o horário inteiro fica livre")
    void cancelarNoInstanteDoInicio() {
        CenarioDeReserva cenario = new CenarioDeReserva(terca(10, 0));
        RecursoPedido sala = cenario.sala(102);
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, APROVADA, terca(10, 0), terca(11, 0), sala);

        cenario.assertMudouPara(cenario.ciclo.cancela(DONO_A, reserva.id()), reserva, CANCELADA);
        assertThat(cenario.ocupado(sala, terca(10, 0), terca(11, 0))).as("horário ocupado").isFalse();
    }

    @Test
    @DisplayName("TC-24 (RN-13-T6): reserva 10:00-12:00 EM_USO encerrada às 10:40 vira CONCLUIDA e continua no histórico")
    void encerramentoAntecipadoComCheckin() {
        CenarioDeReserva cenario = new CenarioDeReserva(terca(10, 40));
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, EM_USO, terca(10, 0), terca(12, 0), cenario.sala(102));

        cenario.assertMudouPara(cenario.ciclo.cancela(DONO_A, reserva.id()), reserva, CONCLUIDA);
        assertThat(cenario.agenda.busca(reserva.id())).as("reserva no histórico").isPresent();
    }

    @Test
    @DisplayName("TC-26 (RN-13-T14): cancelar EM_USO às 10:01 vira CONCLUIDA e o resto do horário fica livre")
    void encerramentoUmMinutoDepoisDoInicio() {
        CenarioDeReserva cenario = new CenarioDeReserva(terca(10, 1));
        RecursoPedido sala = cenario.sala(102);
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, EM_USO, terca(10, 0), terca(11, 0), sala);

        cenario.assertMudouPara(cenario.ciclo.cancela(DONO_A, reserva.id()), reserva, CONCLUIDA);
        assertThat(cenario.ocupado(sala, terca(10, 1), terca(11, 0))).as("resto do horário ocupado").isFalse();
    }

    @Test
    @DisplayName("TC-27 (RN-13-T8): cancelar às 11:30, depois do fim, é recusado")
    void recusaCancelarDepoisDoFim() {
        CenarioDeReserva cenario = new CenarioDeReserva(terca(11, 30));
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, APROVADA, terca(10, 0), terca(11, 0), cenario.sala(102));

        assertRecusada(cenario.ciclo.cancela(DONO_A, reserva.id()), MotivoDeRecusa.NAO_CANCELAVEL);
        assertThat(cenario.estadoDe(reserva)).isEqualTo(APROVADA);
    }

    @Test
    @DisplayName("TC-28: cancelar às 11:00 em ponto, no instante do fim, é recusado ([início, fim))")
    void recusaCancelarNoInstanteDoFim() {
        CenarioDeReserva cenario = new CenarioDeReserva(terca(11, 0));
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, EM_USO, terca(10, 0), terca(11, 0), cenario.sala(102));

        assertRecusada(cenario.ciclo.cancela(DONO_A, reserva.id()), MotivoDeRecusa.NAO_CANCELAVEL);
        assertThat(cenario.estadoDe(reserva)).isEqualTo(EM_USO);
    }

    @ParameterizedTest(name = "TC-29/TC-30: cancelar reserva {0} é recusado")
    @EnumSource(value = EstadoDaReserva.class, names = {"CANCELADA", "CONCLUIDA", "REJEITADA", "NAO_COMPARECEU"})
    @DisplayName("TC-29 e TC-30 (RN-13-T9 e T10): cancelar reserva em estado final é recusado e o estado não muda")
    void recusaCancelarEstadoFinal(EstadoDaReserva estado) {
        CenarioDeReserva cenario = new CenarioDeReserva(terca(10, 30));
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, estado, terca(10, 0), terca(12, 0), cenario.sala(102));

        assertRecusada(cenario.ciclo.cancela(DONO_A, reserva.id()), MotivoDeRecusa.NAO_CANCELAVEL);
        assertThat(cenario.estadoDe(reserva)).isEqualTo(estado);
    }

    @Test
    @DisplayName("TC-31 (RN-13-T11): quem não é o dono não cancela e a reserva fica intacta")
    void naoDonoNaoCancela() {
        CenarioDeReserva cenario = new CenarioDeReserva();
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, APROVADA, terca(10, 0), terca(11, 0), cenario.sala(102));

        assertRecusada(cenario.ciclo.cancela(DONO_B, reserva.id()), MotivoDeRecusa.NAO_DONO);
        cenario.assertMantida(reserva);
    }

    @Test
    @DisplayName("TC-32 (RN-13-T17): quem não é o dono não altera e a reserva fica intacta")
    void naoDonoNaoAltera() {
        CenarioDeReserva cenario = new CenarioDeReserva();
        RecursoPedido sala = cenario.sala(102);
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, APROVADA, terca(10, 0), terca(11, 0), sala);

        DecisaoDaReserva decisao = cenario.regra.altera(DONO_B, reserva.id(),
                CenarioDeReserva.pedido(terca(14, 0), terca(15, 0), sala));

        assertRecusada(decisao, MotivoDeRecusa.NAO_DONO);
        cenario.assertMantida(reserva);
    }

    @Test
    @DisplayName("TC-38 (RN-08-T6): cancelada antes do início, a reserva continua consultável com as datas")
    void canceladaContinuaConsultavel() {
        CenarioDeReserva cenario = new CenarioDeReserva();
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, APROVADA, terca(10, 0), terca(11, 0), cenario.sala(102));

        cenario.assertMudouPara(cenario.ciclo.cancela(DONO_A, reserva.id()), reserva, CANCELADA);

        ReservaNaAgenda guardada = cenario.agenda.busca(reserva.id()).orElseThrow();
        assertThat(guardada.inicio()).isEqualTo(terca(10, 0));
        assertThat(guardada.termino()).isEqualTo(terca(11, 0));
    }

    @Test
    @DisplayName("TC-39 (RN-13-T12): cancelar uma reserva que não existe é recusado como não encontrada")
    void cancelarReservaInexistente() {
        CenarioDeReserva cenario = new CenarioDeReserva();
        assertRecusada(cenario.ciclo.cancela(DONO_A, 999), MotivoDeRecusa.RESERVA_INEXISTENTE);
        assertThat(cenario.agenda.reservas()).isEmpty();
    }
}
