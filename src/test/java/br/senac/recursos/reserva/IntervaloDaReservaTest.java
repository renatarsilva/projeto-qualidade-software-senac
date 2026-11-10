package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.CenarioDeReserva.DONO_A;
import static br.senac.recursos.reserva.CenarioDeReserva.assertAceita;
import static br.senac.recursos.reserva.CenarioDeReserva.assertRecusada;
import static br.senac.recursos.reserva.HorariosDeTeste.segunda;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Intervalo da reserva: término depois do início (spec T-20, CA-01 a CA-03; TC-01 a TC-08; RN-01-T1 a T8). */
class IntervaloDaReservaTest {

    private CenarioDeReserva cenario;
    private RecursoPedido sala;

    @BeforeEach
    void preparar() {
        cenario = new CenarioDeReserva();
        sala = cenario.sala(101);
    }

    @Test
    @DisplayName("TC-01 (RN-01-T1): 10:00 às 11:00 no mesmo dia é aceito e gravado como SOLICITADA")
    void aceitaIntervaloNoMesmoDia() {
        DecisaoDaReserva decisao = cenario.cria(DONO_A, segunda(10, 0), segunda(11, 0), sala);

        assertAceita(decisao);
        assertThat(cenario.agenda.reservas()).hasSize(1);
        assertThat(cenario.agenda.reservas().get(0).estado()).isEqualTo(EstadoDaReserva.SOLICITADA);
    }

    @Test
    @DisplayName("TC-02 (RN-01-T2): 10:00 às 11:00 de um dia futuro é aceito (data e hora comparadas)")
    void aceitaIntervaloEmDiaFuturo() {
        assertAceita(cenario.cria(DONO_A, terca(10, 0), terca(11, 0), sala));
    }

    @Test
    @DisplayName("TC-03 (RN-01-T3): 10:00 às 10:01, a menor duração positiva, é aceito")
    void aceitaDuracaoDeUmMinuto() {
        assertAceita(cenario.cria(DONO_A, terca(10, 0), terca(10, 1), sala));
    }

    @Test
    @DisplayName("TC-04 (RN-01-T4): término igual ao início é recusado, com o erro no campo termino")
    void recusaTerminoIgualAoInicio() {
        DecisaoDaReserva decisao = cenario.cria(DONO_A, terca(10, 0), terca(10, 0), sala);

        assertRecusada(decisao, MotivoDeRecusa.INTERVALO_INVALIDO);
        assertThat(decisao.campo()).isEqualTo("termino");
        assertThat(decisao.mensagem()).isEqualTo("deve ser posterior ao início");
        cenario.assertNadaGravado(0);
    }

    @Test
    @DisplayName("TC-05 (RN-01-T5): término antes do início é recusado")
    void recusaTerminoAntesDoInicio() {
        DecisaoDaReserva decisao = cenario.cria(DONO_A, terca(10, 0), terca(9, 0), sala);

        assertRecusada(decisao, MotivoDeRecusa.INTERVALO_INVALIDO);
        assertThat(decisao.campo()).isEqualTo("termino");
        cenario.assertNadaGravado(0);
    }

    @Test
    @DisplayName("TC-06 (RN-01-T6): início ausente é recusado com mensagem compreensível")
    void recusaInicioAusente() {
        DecisaoDaReserva decisao = cenario.cria(DONO_A, null, terca(11, 0), sala);

        assertRecusada(decisao, MotivoDeRecusa.DADO_AUSENTE);
        assertThat(decisao.campo()).isEqualTo("inicio");
        assertThat(decisao.mensagem()).isEqualTo("deve ser informado");
        cenario.assertNadaGravado(0);
    }

    @Test
    @DisplayName("TC-07 (RN-01-T7): término ausente é recusado com mensagem compreensível")
    void recusaTerminoAusente() {
        DecisaoDaReserva decisao = cenario.cria(DONO_A, terca(10, 0), null, sala);

        assertRecusada(decisao, MotivoDeRecusa.DADO_AUSENTE);
        assertThat(decisao.campo()).isEqualTo("termino");
        assertThat(decisao.mensagem()).isEqualTo("deve ser informado");
        cenario.assertNadaGravado(0);
    }

    @Test
    @DisplayName("TC-08 (RN-01-T8): alterar para término antes do início é recusado e mantém o horário original")
    void alteracaoComTerminoAntesDoInicioMantemOHorario() {
        ReservaNaAgenda original = cenario.reserva(DONO_A, EstadoDaReserva.SOLICITADA, terca(10, 0), terca(11, 0), sala);

        DecisaoDaReserva decisao = cenario.altera(original, terca(10, 0), terca(9, 0), sala);

        assertRecusada(decisao, MotivoDeRecusa.INTERVALO_INVALIDO);
        assertThat(decisao.campo()).isEqualTo("termino");
        cenario.assertMantida(original);
    }
}
