package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.CenarioDeReserva.DONO_A;
import static br.senac.recursos.reserva.CenarioDeReserva.assertAceita;
import static br.senac.recursos.reserva.CenarioDeReserva.assertRecusada;
import static br.senac.recursos.reserva.EstadoDaReserva.APROVADA;
import static br.senac.recursos.reserva.EstadoDaReserva.CANCELADA;
import static br.senac.recursos.reserva.EstadoDaReserva.SOLICITADA;
import static br.senac.recursos.reserva.HorariosDeTeste.quarta;
import static br.senac.recursos.reserva.HorariosDeTeste.segunda;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Prazos de aprovação e de alteração (spec T-21, CA-13 e CA-15; TC-45 a TC-49, TC-58 a TC-60 e TC-80 a TC-82).
 * Reserva padrão: terça, 10:00 às 11:00. Pedida com mais de 24 h de antecedência, o prazo é segunda 10:00;
 * pedida com menos, é terça 09:00.
 */
class PrazosDaReservaTest {

    private static final Instant PRAZO_DE_24_H = segunda(10, 0);
    private static final Instant PRAZO_DE_1_H = terca(9, 0);

    private ReservaNaAgenda reserva;

    private CenarioDeReserva cenario(Instant agora, EstadoDaReserva estado, Instant prazo) {
        CenarioDeReserva cenario = new CenarioDeReserva(agora);
        reserva = cenario.reservaComPrazo(DONO_A, estado, terca(10, 0), terca(11, 0), prazo, cenario.sala(102));
        return cenario;
    }

    private DecisaoDaReserva alteraParaQuarta(CenarioDeReserva cenario) {
        return cenario.altera(reserva, quarta(10, 0), quarta(11, 0), reserva.recursos().toArray(new RecursoPedido[0]));
    }

    @Test
    @DisplayName("TC-45: pedido com mais de 24 h; o dono altera exatamente 24 h antes do início e é aceito")
    void aceitaAlterarNoPrazoDe24Horas() {
        CenarioDeReserva cenario = cenario(PRAZO_DE_24_H, SOLICITADA, PRAZO_DE_24_H);

        assertAceita(alteraParaQuarta(cenario));
    }

    @Test
    @DisplayName("TC-46: a mesma reserva alterada 23 h 59 min antes é recusada e não muda")
    void recusaAlterarDepoisDoPrazoDe24Horas() {
        CenarioDeReserva cenario = cenario(segunda(10, 1), SOLICITADA, PRAZO_DE_24_H);

        assertRecusada(alteraParaQuarta(cenario), MotivoDeRecusa.NAO_ALTERAVEL);
        cenario.assertMantida(reserva);
    }

    @Test
    @DisplayName("TC-47: pedido com menos de 24 h; o dono altera exatamente 1 h antes e é aceito")
    void aceitaAlterarNoPrazoDe1Hora() {
        CenarioDeReserva cenario = cenario(PRAZO_DE_1_H, SOLICITADA, PRAZO_DE_1_H);

        assertAceita(alteraParaQuarta(cenario));
    }

    @Test
    @DisplayName("TC-48: pedido com menos de 24 h; alterar 59 min antes é recusado e a reserva não muda")
    void recusaAlterarDepoisDoPrazoDe1Hora() {
        CenarioDeReserva cenario = cenario(terca(9, 1), SOLICITADA, PRAZO_DE_1_H);

        assertRecusada(alteraParaQuarta(cenario), MotivoDeRecusa.NAO_ALTERAVEL);
        cenario.assertMantida(reserva);
    }

    @Test
    @DisplayName("TC-49: SOLICITADA pedida com mais de 24 h, sem aprovação depois do prazo, é cancelada pelo sistema")
    void sistemaCancelaDepoisDoPrazoDe24Horas() {
        CenarioDeReserva cenario = cenario(segunda(10, 1), SOLICITADA, PRAZO_DE_24_H);

        cenario.assertMudouPara(cenario.ciclo.muda(reserva.id(), CANCELADA), reserva, CANCELADA);
    }

    @Test
    @DisplayName("TC-58: o Responsável aprova exatamente 24 h antes do início (pedido com mais de 24 h) e a reserva vira APROVADA")
    void aprovaNoPrazoDe24Horas() {
        CenarioDeReserva cenario = cenario(PRAZO_DE_24_H, SOLICITADA, PRAZO_DE_24_H);

        cenario.assertMudouPara(cenario.ciclo.muda(reserva.id(), APROVADA), reserva, APROVADA);
    }

    @Test
    @DisplayName("TC-59: aprovar depois do prazo, antes de o job rodar, é recusado; o job a cancela depois")
    void recusaAprovarDepoisDoPrazo() {
        CenarioDeReserva cenario = cenario(segunda(10, 1), SOLICITADA, PRAZO_DE_24_H);

        assertRecusada(cenario.ciclo.muda(reserva.id(), APROVADA), MotivoDeRecusa.PRAZO_DE_APROVACAO_VENCIDO);
        assertThat(cenario.estadoDe(reserva)).isEqualTo(SOLICITADA);
        cenario.assertMudouPara(cenario.ciclo.muda(reserva.id(), CANCELADA), reserva, CANCELADA);
    }

    @Test
    @DisplayName("TC-60: alterar uma APROVADA 24 h antes, para um início 48 h adiante, volta a SOLICITADA com o prazo recalculado")
    void alterarAprovadaRecalculaOPrazo() {
        CenarioDeReserva cenario = cenario(PRAZO_DE_24_H, APROVADA, PRAZO_DE_24_H);
        Instant novoInicio = terca(10, 0).plus(Duration.ofHours(48));

        DecisaoDaReserva decisao = cenario.altera(reserva, novoInicio, novoInicio.plus(Duration.ofHours(1)),
                reserva.recursos().toArray(new RecursoPedido[0]));

        cenario.assertMudouPara(decisao, reserva, SOLICITADA);
        assertThat(decisao.reserva().prazoAprovacaoEm()).isEqualTo(novoInicio.minus(Duration.ofHours(24)));
    }

    @ParameterizedTest(name = "TC-80: pedido às {0} para terça 10:00 tem prazo {1}")
    @CsvSource({"2026-11-09T08:00:00Z, 2026-11-09T10:00:00Z", "2026-11-09T16:00:00Z, 2026-11-10T09:00:00Z"})
    @DisplayName("TC-80 (RN-14-T1 e T2): o prazo na criação é início menos 24 h ou, com menos de 24 h, menos 1 h")
    void prazoNaCriacao(Instant pedidoEm, Instant prazoEsperado) {
        CenarioDeReserva cenario = new CenarioDeReserva(pedidoEm);

        DecisaoDaReserva decisao = cenario.cria(DONO_A, terca(10, 0), terca(11, 0), cenario.sala(102));

        assertAceita(decisao);
        assertThat(decisao.reserva().prazoAprovacaoEm()).isEqualTo(prazoEsperado);
    }

    @Test
    @DisplayName("TC-81 (RN-14-T7): o job no instante exato do prazo não cancela; um segundo depois, cancela")
    void jobSoCancelaDepoisDoPrazo() {
        CenarioDeReserva noPrazo = cenario(PRAZO_DE_24_H, SOLICITADA, PRAZO_DE_24_H);
        assertRecusada(noPrazo.ciclo.muda(reserva.id(), CANCELADA), MotivoDeRecusa.TRANSICAO_PROIBIDA);
        assertThat(noPrazo.estadoDe(reserva)).isEqualTo(SOLICITADA);

        CenarioDeReserva depois = cenario(PRAZO_DE_24_H.plusSeconds(1), SOLICITADA, PRAZO_DE_24_H);
        depois.assertMudouPara(depois.ciclo.muda(reserva.id(), CANCELADA), reserva, CANCELADA);
    }

    @Test
    @DisplayName("TC-82 (RN-14-T5): o Responsável aprova exatamente em prazo_aprovacao_em (pedido com menos de 24 h) e é aceito")
    void aprovaExatamenteNoPrazo() {
        CenarioDeReserva cenario = cenario(PRAZO_DE_1_H, SOLICITADA, PRAZO_DE_1_H);

        cenario.assertMudouPara(cenario.ciclo.muda(reserva.id(), APROVADA), reserva, APROVADA);
    }
}
