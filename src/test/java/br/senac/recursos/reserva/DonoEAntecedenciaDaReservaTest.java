package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.CenarioDeReserva.DONO_A;
import static br.senac.recursos.reserva.CenarioDeReserva.assertAceita;
import static br.senac.recursos.reserva.CenarioDeReserva.assertRecusada;
import static br.senac.recursos.reserva.HorariosDeTeste.AGORA;
import static br.senac.recursos.reserva.HorariosDeTeste.segunda;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Dono da reserva e antecedência mínima de 1 h (spec T-20, CA-14 e CA-15; TC-42, TC-45, TC-46, TC-51 a TC-54; RN-15). */
class DonoEAntecedenciaDaReservaTest {

    private static final String MENSAGEM = "A reserva precisa ser pedida com pelo menos 1 hora de antecedência";

    @Test
    @DisplayName("TC-42: o dono da reserva é o usuário autenticado")
    void donoEOUsuarioAutenticado() {
        CenarioDeReserva cenario = new CenarioDeReserva();

        DecisaoDaReserva decisao = cenario.cria(7, terca(10, 0), terca(11, 0), cenario.sala(101));

        assertAceita(decisao);
        assertThat(decisao.reserva().donoId()).isEqualTo(7);
    }

    @Test
    @DisplayName("TC-45 (RN-15-T1): Clock em 09:00 e início às 10:00, exatamente 1 h antes, é aceito")
    void aceitaComExatamenteUmaHora() {
        CenarioDeReserva cenario = new CenarioDeReserva(segunda(9, 0));

        assertAceita(cenario.cria(DONO_A, segunda(10, 0), segunda(11, 0), cenario.sala(101)));
    }

    @Test
    @DisplayName("TC-46 (RN-15-T2): Clock em 09:01 e início às 10:00, 59 min antes, é recusado e nada é gravado")
    void recusaComCinquentaENoveMinutos() {
        CenarioDeReserva cenario = new CenarioDeReserva(segunda(9, 1));

        DecisaoDaReserva decisao = cenario.cria(DONO_A, segunda(10, 0), segunda(11, 0), cenario.sala(101));

        assertRecusada(decisao, MotivoDeRecusa.ANTECEDENCIA_INSUFICIENTE);
        assertThat(decisao.mensagem()).isEqualTo(MENSAGEM);
        cenario.assertNadaGravado(0);
    }

    @Test
    @DisplayName("RN-15-T3 (era RN-12-T3): início exatamente agora é recusado")
    void recusaInicioExatamenteAgora() {
        CenarioDeReserva cenario = new CenarioDeReserva();

        DecisaoDaReserva decisao = cenario.cria(DONO_A, AGORA, AGORA.plus(Duration.ofHours(1)), cenario.sala(101));

        assertRecusada(decisao, MotivoDeRecusa.ANTECEDENCIA_INSUFICIENTE);
        cenario.assertNadaGravado(0);
    }

    @Test
    @DisplayName("TC-54 (RN-15-T4): Clock em 08:00 e início às 07:00, no passado, é recusado e nada é gravado")
    void recusaInicioNoPassado() {
        CenarioDeReserva cenario = new CenarioDeReserva();

        DecisaoDaReserva decisao = cenario.cria(DONO_A, segunda(7, 0), segunda(8, 0), cenario.sala(101));

        assertRecusada(decisao, MotivoDeRecusa.ANTECEDENCIA_INSUFICIENTE);
        cenario.assertNadaGravado(0);
    }

    @Test
    @DisplayName("TC-51 (RN-15-T6): reserva antiga alterada para começar 30 min depois de agora é recusada e não muda")
    void recusaAlteracaoParaTrintaMinutosDepoisDeAgora() {
        CenarioDeReserva cenario = new CenarioDeReserva();
        RecursoPedido sala = cenario.sala(101);
        ReservaNaAgenda antiga = cenario.reserva(DONO_A, EstadoDaReserva.SOLICITADA, terca(10, 0), terca(11, 0), sala);

        DecisaoDaReserva decisao = cenario.altera(antiga, segunda(8, 30), segunda(9, 30), sala);

        assertRecusada(decisao, MotivoDeRecusa.ANTECEDENCIA_INSUFICIENTE);
        assertThat(decisao.mensagem()).isEqualTo(MENSAGEM);
        cenario.assertMantida(antiga);
    }

    @Test
    @DisplayName("TC-52: a mesma reserva alterada para começar exatamente 1 h depois de agora é aceita")
    void aceitaAlteracaoParaExatamenteUmaHoraDepois() {
        CenarioDeReserva cenario = new CenarioDeReserva();
        RecursoPedido sala = cenario.sala(101);
        ReservaNaAgenda antiga = cenario.reserva(DONO_A, EstadoDaReserva.SOLICITADA, terca(10, 0), terca(11, 0), sala);

        assertAceita(cenario.altera(antiga, segunda(9, 0), segunda(10, 0), sala));
    }

    @ParameterizedTest(name = "TC-53 (RN-14-T8, RN-15-T5): APROVADA alterada com {0} h de antecedência: prazo = início - {1} h")
    @CsvSource({"48, 24", "3, 1"})
    @DisplayName("TC-53: alterar uma APROVADA volta a SOLICITADA e recalcula o prazo_aprovacao_em com o novo início")
    void alteracaoRecalculaOPrazoDeAprovacao(long horasDeAntecedencia, long horasDoPrazo) {
        CenarioDeReserva cenario = new CenarioDeReserva();
        RecursoPedido sala = cenario.sala(101);
        ReservaNaAgenda aprovada = cenario.reserva(DONO_A, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala);
        Instant novoInicio = AGORA.plus(Duration.ofHours(horasDeAntecedencia));

        DecisaoDaReserva decisao = cenario.altera(aprovada, novoInicio, novoInicio.plus(Duration.ofHours(1)), sala);

        assertAceita(decisao);
        assertThat(decisao.reserva().estado()).isEqualTo(EstadoDaReserva.SOLICITADA);
        assertThat(decisao.reserva().prazoAprovacaoEm()).isEqualTo(novoInicio.minus(Duration.ofHours(horasDoPrazo)));
    }
}
