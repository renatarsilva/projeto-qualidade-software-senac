package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.CenarioDeReserva.DONO_A;
import static br.senac.recursos.reserva.CenarioDeReserva.DONO_B;
import static br.senac.recursos.reserva.CenarioDeReserva.assertAceita;
import static br.senac.recursos.reserva.CenarioDeReserva.assertRecusada;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Só os estados ativos ocupam o horário (spec T-20, CA-08, S-02; TC-21 a TC-26; RN-02-T7 a T11, T14 e T15). */
class EstadosQueOcupamOHorarioTest {

    private CenarioDeReserva cenario;
    private RecursoPedido sala;

    @BeforeEach
    void preparar() {
        cenario = new CenarioDeReserva();
        sala = cenario.sala(102);
    }

    @ParameterizedTest(name = "existente {0}: B pede 10:30 às 11:30 e é recusado")
    @EnumSource(value = EstadoDaReserva.class, names = {"SOLICITADA", "APROVADA", "EM_USO"})
    @DisplayName("TC-21 e TC-22 (RN-02-T7, T8 e T15): SOLICITADA, APROVADA e EM_USO ocupam o horário")
    void estadosAtivosOcupam(EstadoDaReserva estado) {
        cenario.reserva(DONO_A, estado, terca(10, 0), terca(11, 0), sala);

        DecisaoDaReserva decisao = cenario.cria(DONO_B, terca(10, 30), terca(11, 30), sala);

        assertRecusada(decisao, MotivoDeRecusa.SOBREPOSICAO);
        cenario.assertNadaGravado(1);
    }

    @ParameterizedTest(name = "existente {0}: B pede 10:30 às 11:30 e é aceito")
    @EnumSource(value = EstadoDaReserva.class,
            names = {"CANCELADA", "REJEITADA", "NAO_COMPARECEU", "CONCLUIDA"})
    @DisplayName("TC-23 a TC-26 (RN-02-T9, T10, T11 e T14): CANCELADA, REJEITADA, NAO_COMPARECEU e CONCLUIDA não ocupam")
    void estadosEncerradosNaoOcupam(EstadoDaReserva estado) {
        cenario.reserva(DONO_A, estado, terca(10, 0), terca(11, 0), sala);

        assertAceita(cenario.cria(DONO_B, terca(10, 30), terca(11, 30), sala));
    }

    @Test
    @DisplayName("RN-02-T15: B pede 10:30 às 11:00 e a existente é APROVADA sem check-in: recusado, sem tolerância de atraso")
    void aprovadaSemCheckinOcupaOHorarioInteiro() {
        cenario.reserva(DONO_A, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala);

        assertRecusada(cenario.cria(DONO_B, terca(10, 30), terca(11, 0), sala), MotivoDeRecusa.SOBREPOSICAO);
    }
}
