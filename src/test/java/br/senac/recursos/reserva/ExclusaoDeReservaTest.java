package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.CenarioDeReserva.DONO_A;
import static br.senac.recursos.reserva.CenarioDeReserva.assertRecusada;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Nenhuma reserva é apagada (spec T-21, CA-11 e CA-12; TC-33 a TC-37 e TC-39). Reserva padrão: terça, 10:00 às 11:00. */
class ExclusaoDeReservaTest {

    private void recusaApagar(Instant agora, EstadoDaReserva estado) {
        CenarioDeReserva cenario = new CenarioDeReserva(agora);
        ReservaNaAgenda reserva = cenario.reserva(DONO_A, estado, terca(10, 0), terca(11, 0), cenario.sala(102));

        assertRecusada(cenario.ciclo.apaga(reserva.id()), MotivoDeRecusa.NAO_APAGAVEL);
        cenario.assertMantida(reserva);
    }

    @Test
    @DisplayName("TC-33 (RN-08-T1): apagar uma reserva EM_USO é recusado")
    void recusaApagarEmUso() {
        recusaApagar(terca(10, 30), EstadoDaReserva.EM_USO);
    }

    @Test
    @DisplayName("TC-34 (RN-08-T2): apagar uma reserva CONCLUIDA é recusado")
    void recusaApagarConcluida() {
        recusaApagar(terca(12, 0), EstadoDaReserva.CONCLUIDA);
    }

    @Test
    @DisplayName("TC-35 (RN-08-T3): apagar às 10:00 em ponto é recusado")
    void recusaApagarNoInstanteDoInicio() {
        recusaApagar(terca(10, 0), EstadoDaReserva.APROVADA);
    }

    @Test
    @DisplayName("TC-36 (RN-08-T4): apagar às 09:59, antes do início, é recusado; o caminho é cancelar")
    void recusaApagarAntesDoInicio() {
        recusaApagar(terca(9, 59), EstadoDaReserva.APROVADA);
    }

    @ParameterizedTest(name = "TC-37: apagar reserva {0} é recusado")
    @EnumSource(value = EstadoDaReserva.class, names = {"CANCELADA", "REJEITADA", "NAO_COMPARECEU"})
    @DisplayName("TC-37 (RN-08-T5): apagar reserva CANCELADA, REJEITADA ou NAO_COMPARECEU é recusado")
    void recusaApagarEstadosFinais(EstadoDaReserva estado) {
        recusaApagar(terca(12, 0), estado);
    }

    @Test
    @DisplayName("TC-39 (RN-08-T8): apagar uma reserva que não existe é recusado como não encontrada")
    void apagarReservaInexistente() {
        CenarioDeReserva cenario = new CenarioDeReserva();

        assertRecusada(cenario.ciclo.apaga(999), MotivoDeRecusa.RESERVA_INEXISTENTE);
        assertThat(cenario.agenda.reservas()).isEmpty();
    }
}
