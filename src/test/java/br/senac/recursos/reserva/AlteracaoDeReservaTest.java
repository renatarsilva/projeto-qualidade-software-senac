package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.CenarioDeReserva.DONO_A;
import static br.senac.recursos.reserva.CenarioDeReserva.DONO_B;
import static br.senac.recursos.reserva.CenarioDeReserva.DONO_C;
import static br.senac.recursos.reserva.CenarioDeReserva.assertAceita;
import static br.senac.recursos.reserva.CenarioDeReserva.assertRecusada;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Alteração de reserva (spec T-20, CA-04, CA-09, CA-11, CA-13 e CA-17; TC-15 a TC-18, TC-32 a TC-34, TC-39, TC-40,
 * TC-59 e TC-62; RN-02-T1 a T4).
 */
class AlteracaoDeReservaTest {

    private CenarioDeReserva cenario;
    private RecursoPedido sala101;
    private RecursoPedido sala102;

    @BeforeEach
    void preparar() {
        cenario = new CenarioDeReserva();
        sala101 = cenario.sala(101);
        sala102 = cenario.sala(102);
    }

    @Test
    @DisplayName("TC-15 (RN-02-T1): estender para 10:00-12:00 com B na sala das 11:00 às 12:00 é recusado")
    void recusaExtensaoQueBateEmOutraReserva() {
        ReservaNaAgenda deA = cenario.reserva(DONO_A, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala102);
        cenario.reserva(DONO_B, EstadoDaReserva.APROVADA, terca(11, 0), terca(12, 0), sala102);

        DecisaoDaReserva decisao = cenario.altera(deA, terca(10, 0), terca(12, 0), sala102);

        assertRecusada(decisao, MotivoDeRecusa.SOBREPOSICAO);
        cenario.assertMantida(deA);
    }

    @Test
    @DisplayName("TC-16 (RN-02-T2): estender para 10:00-12:00 com a sala livre depois é aceito (não conflita consigo)")
    void aceitaExtensaoComSalaLivre() {
        ReservaNaAgenda deA = cenario.reserva(DONO_A, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala102);

        assertAceita(cenario.altera(deA, terca(10, 0), terca(12, 0), sala102));
    }

    @Test
    @DisplayName("TC-17 (RN-02-T3): estender de 10:00-10:30 para 10:00-11:00 com B das 11:00 às 12:00 é aceito")
    void aceitaExtensaoQueEncostaEmOutraReserva() {
        ReservaNaAgenda deA = cenario.reserva(DONO_A, EstadoDaReserva.APROVADA, terca(10, 0), terca(10, 30), sala102);
        cenario.reserva(DONO_B, EstadoDaReserva.APROVADA, terca(11, 0), terca(12, 0), sala102);

        assertAceita(cenario.altera(deA, terca(10, 0), terca(11, 0), sala102));
    }

    @Test
    @DisplayName("TC-18 (RN-02-T4): mudar para 13:00-14:00, já ocupado por C, é recusado e mantém o horário")
    void recusaMudancaParaHorarioOcupado() {
        ReservaNaAgenda deA = cenario.reserva(DONO_A, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala102);
        cenario.reserva(DONO_C, EstadoDaReserva.APROVADA, terca(13, 0), terca(14, 0), sala102);

        DecisaoDaReserva decisao = cenario.altera(deA, terca(13, 0), terca(14, 0), sala102);

        assertRecusada(decisao, MotivoDeRecusa.SOBREPOSICAO);
        cenario.assertMantida(deA);
    }

    @Test
    @DisplayName("TC-32: alterar uma APROVADA para horário livre a devolve a SOLICITADA; o horário antigo libera e o novo ocupa")
    void alterarAprovadaVoltaParaSolicitada() {
        ReservaNaAgenda deA = cenario.reserva(DONO_A, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala102);

        DecisaoDaReserva decisao = cenario.altera(deA, terca(14, 0), terca(15, 0), sala102);

        assertAceita(decisao);
        assertThat(decisao.reserva().estado()).isEqualTo(EstadoDaReserva.SOLICITADA);
        assertAceita(cenario.cria(DONO_B, terca(10, 0), terca(11, 0), sala102));
        assertRecusada(cenario.cria(DONO_C, terca(14, 0), terca(15, 0), sala102), MotivoDeRecusa.SOBREPOSICAO);
    }

    @Test
    @DisplayName("TC-33: alterar uma SOLICITADA para horário livre continua SOLICITADA")
    void alterarSolicitadaContinuaSolicitada() {
        ReservaNaAgenda deA = cenario.reserva(DONO_A, EstadoDaReserva.SOLICITADA, terca(10, 0), terca(11, 0), sala102);

        DecisaoDaReserva decisao = cenario.altera(deA, terca(14, 0), terca(15, 0), sala102);

        assertAceita(decisao);
        assertThat(decisao.reserva().estado()).isEqualTo(EstadoDaReserva.SOLICITADA);
    }

    @Test
    @DisplayName("TC-34: alterar uma APROVADA para horário ocupado é recusado e ela continua APROVADA no horário original")
    void alterarAprovadaParaHorarioOcupadoContinuaAprovada() {
        ReservaNaAgenda deA = cenario.reserva(DONO_A, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala102);
        cenario.reserva(DONO_C, EstadoDaReserva.APROVADA, terca(13, 0), terca(14, 0), sala102);

        DecisaoDaReserva decisao = cenario.altera(deA, terca(13, 0), terca(14, 0), sala102);

        assertRecusada(decisao, MotivoDeRecusa.SOBREPOSICAO);
        cenario.assertMantida(deA);
        assertThat(cenario.agenda.busca(deA.id()).orElseThrow().estado()).isEqualTo(EstadoDaReserva.APROVADA);
    }

    @Test
    @DisplayName("TC-39: alterar uma reserva EM_USO é recusado e ela não muda")
    void recusaAlterarReservaEmUso() {
        ReservaNaAgenda deA = cenario.reserva(DONO_A, EstadoDaReserva.EM_USO, terca(10, 0), terca(11, 0), sala102);

        DecisaoDaReserva decisao = cenario.altera(deA, terca(14, 0), terca(15, 0), sala102);

        assertRecusada(decisao, MotivoDeRecusa.NAO_ALTERAVEL);
        cenario.assertMantida(deA);
    }

    @ParameterizedTest(name = "TC-40: alterar reserva {0} é recusado")
    @EnumSource(value = EstadoDaReserva.class, names = {"CONCLUIDA", "CANCELADA", "REJEITADA", "NAO_COMPARECEU"})
    @DisplayName("TC-40: reservas em estado final não podem ser alteradas")
    void recusaAlterarReservaEmEstadoFinal(EstadoDaReserva estado) {
        ReservaNaAgenda deA = cenario.reserva(DONO_A, estado, terca(10, 0), terca(11, 0), sala102);

        DecisaoDaReserva decisao = cenario.altera(deA, terca(14, 0), terca(15, 0), sala102);

        assertRecusada(decisao, MotivoDeRecusa.NAO_ALTERAVEL);
        cenario.assertMantida(deA);
    }

    @Test
    @DisplayName("TC-59: trocar a sala 101 pela 102, ocupada no horário, é recusado e a reserva fica na 101")
    void recusaTrocaParaSalaOcupada() {
        ReservaNaAgenda deA = cenario.reserva(DONO_A, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala101);
        cenario.reserva(DONO_B, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala102);

        DecisaoDaReserva decisao = cenario.altera(deA, terca(10, 0), terca(11, 0), sala102);

        assertRecusada(decisao, MotivoDeRecusa.SOBREPOSICAO);
        cenario.assertMantida(deA);
    }

    @Test
    @DisplayName("TC-62: mudar só o término, mantendo sala e professor, é aceito (os recursos da própria reserva não conflitam)")
    void aceitaMudarSoOTerminoMantendoOsRecursos() {
        RecursoPedido professor = cenario.professor(7);
        ReservaNaAgenda deA = cenario.reserva(DONO_A, EstadoDaReserva.SOLICITADA, terca(10, 0), terca(11, 0),
                sala102, professor);

        assertAceita(cenario.altera(deA, terca(10, 0), terca(12, 0), sala102, professor));
    }
}
