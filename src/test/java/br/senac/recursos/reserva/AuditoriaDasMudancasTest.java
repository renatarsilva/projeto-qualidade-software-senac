package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.CenarioDeReserva.DONO_A;
import static br.senac.recursos.reserva.CenarioDeReserva.DONO_B;
import static br.senac.recursos.reserva.CenarioDeReserva.assertAceita;
import static br.senac.recursos.reserva.EstadoDaReserva.APROVADA;
import static br.senac.recursos.reserva.EstadoDaReserva.CANCELADA;
import static br.senac.recursos.reserva.EstadoDaReserva.CONCLUIDA;
import static br.senac.recursos.reserva.EstadoDaReserva.EM_USO;
import static br.senac.recursos.reserva.EstadoDaReserva.NAO_COMPARECEU;
import static br.senac.recursos.reserva.EstadoDaReserva.REJEITADA;
import static br.senac.recursos.reserva.EstadoDaReserva.SOLICITADA;
import static br.senac.recursos.reserva.HorariosDeTeste.AGORA;
import static br.senac.recursos.reserva.HorariosDeTeste.quarta;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;

import br.senac.recursos.auditoria.RegistroDeAuditoria;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cada mudança de estado deixa um registro (spec T-40, CA-01 a CA-04 e CA-06; TC-01, TC-03 a TC-09, TC-12).
 * Reserva padrão: terça, 10:00 às 11:00. O Responsável é o usuário 9.
 */
class AuditoriaDasMudancasTest {

    private static final long RESPONSAVEL = 9;

    private CenarioDeReserva cenario;
    private RecursoPedido sala;

    private ReservaNaAgenda reservaEm(Instant agora, EstadoDaReserva estado) {
        cenario = new CenarioDeReserva(agora);
        sala = cenario.sala(102);
        return cenario.reserva(DONO_A, estado, terca(10, 0), terca(11, 0), sala);
    }

    private void assertUmRegistro(ReservaNaAgenda reserva, Long usuario, EstadoDaReserva anterior,
            EstadoDaReserva novo, String motivo) {
        List<RegistroDeAuditoria> historico = cenario.auditoria.historico(reserva.id());
        assertThat(historico).as("registros da reserva").hasSize(1);
        RegistroDeAuditoria r = historico.get(0);
        assertThat(r.usuarioId()).as("usuário").isEqualTo(usuario);
        assertThat(r.estadoAnterior()).as("estado anterior").isEqualTo(anterior);
        assertThat(r.estadoNovo()).as("estado novo").isEqualTo(novo);
        assertThat(r.motivo()).as("motivo").isEqualTo(motivo);
    }

    @Test
    @DisplayName("TC-01 e TC-12 (RN-09-T1 e T9): criar a reserva grava 1 registro: anterior vazio, novo SOLICITADA, dono e instante do Clock")
    void criacaoGravaORegistro() {
        cenario = new CenarioDeReserva();
        DecisaoDaReserva decisao = cenario.cria(DONO_A, terca(10, 0), terca(11, 0), cenario.sala(102));

        assertAceita(decisao);
        assertUmRegistro(decisao.reserva(), DONO_A, null, SOLICITADA, null);
        assertThat(cenario.auditoria.todos().get(0).ocorridaEm()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("TC-03 (RN-09-T3): o Responsável aprova: SOLICITADA para APROVADA, com o Responsável como usuário")
    void aprovacao() {
        ReservaNaAgenda reserva = reservaEm(AGORA, SOLICITADA);

        assertAceita(cenario.ciclo.muda(reserva.id(), APROVADA, RESPONSAVEL));

        assertUmRegistro(reserva, RESPONSAVEL, SOLICITADA, APROVADA, null);
    }

    @Test
    @DisplayName("TC-03: o Responsável rejeita: SOLICITADA para REJEITADA")
    void rejeicao() {
        ReservaNaAgenda reserva = reservaEm(AGORA, SOLICITADA);

        assertAceita(cenario.ciclo.muda(reserva.id(), REJEITADA, RESPONSAVEL));

        assertUmRegistro(reserva, RESPONSAVEL, SOLICITADA, REJEITADA, null);
    }

    @Test
    @DisplayName("TC-03: o check-in do dono: APROVADA para EM_USO, com o dono como usuário")
    void checkin() {
        ReservaNaAgenda reserva = reservaEm(terca(10, 30), APROVADA);

        assertAceita(cenario.ciclo.checkin(DONO_A, reserva.id()));

        assertUmRegistro(reserva, DONO_A, APROVADA, EM_USO, null);
    }

    @Test
    @DisplayName("TC-03: o dono cancela antes do início: APROVADA para CANCELADA, sem motivo")
    void cancelamentoAntesDoInicio() {
        ReservaNaAgenda reserva = reservaEm(AGORA, APROVADA);

        assertAceita(cenario.ciclo.cancela(DONO_A, reserva.id()));

        assertUmRegistro(reserva, DONO_A, APROVADA, CANCELADA, null);
    }

    @Test
    @DisplayName("TC-03: o dono cancela depois do início sem check-in: APROVADA para NAO_COMPARECEU, com o motivo")
    void cancelamentoSemCheckin() {
        ReservaNaAgenda reserva = reservaEm(terca(10, 5), APROVADA);

        assertAceita(cenario.ciclo.cancela(DONO_A, reserva.id()));

        assertUmRegistro(reserva, DONO_A, APROVADA, NAO_COMPARECEU, "cancelamento sem check-in");
    }

    @Test
    @DisplayName("TC-03: o dono encerra antes do fim: EM_USO para CONCLUIDA, com o motivo")
    void encerramentoAntecipado() {
        ReservaNaAgenda reserva = reservaEm(terca(10, 40), EM_USO);

        assertAceita(cenario.ciclo.cancela(DONO_A, reserva.id()));

        assertUmRegistro(reserva, DONO_A, EM_USO, CONCLUIDA, "encerramento antecipado pelo dono");
    }

    @Test
    @DisplayName("TC-04 (RN-09-T4): o sistema encerra no fim: EM_USO para CONCLUIDA, usuário vazio")
    void sistemaConclui() {
        ReservaNaAgenda reserva = reservaEm(terca(11, 0), EM_USO);

        assertAceita(cenario.ciclo.muda(reserva.id(), CONCLUIDA));

        assertUmRegistro(reserva, null, EM_USO, CONCLUIDA, null);
    }

    @Test
    @DisplayName("TC-04: o sistema marca NAO_COMPARECEU no fim, sem check-in: usuário vazio")
    void sistemaMarcaNaoCompareceu() {
        ReservaNaAgenda reserva = reservaEm(terca(11, 0), APROVADA);

        assertAceita(cenario.ciclo.muda(reserva.id(), NAO_COMPARECEU));

        assertUmRegistro(reserva, null, APROVADA, NAO_COMPARECEU, null);
    }

    @Test
    @DisplayName("TC-05 (RN-09-T5): cancelamento por falta de aprovação: motivo \"não aprovada a tempo\" e usuário vazio")
    void cancelamentoPorPrazo() {
        cenario = new CenarioDeReserva(terca(9, 1));
        ReservaNaAgenda reserva = cenario.reservaComPrazo(DONO_A, SOLICITADA, terca(10, 0), terca(11, 0),
                terca(9, 0), cenario.sala(102));

        assertAceita(cenario.ciclo.muda(reserva.id(), CANCELADA));

        assertUmRegistro(reserva, null, SOLICITADA, CANCELADA, "não aprovada a tempo");
    }

    @Test
    @DisplayName("TC-06: o dono altera uma APROVADA: registro APROVADA para SOLICITADA, com o motivo")
    void alteracaoDeAprovadaGravaORegistro() {
        ReservaNaAgenda reserva = reservaEm(AGORA, APROVADA);

        assertAceita(cenario.altera(reserva, quarta(10, 0), quarta(11, 0), sala));

        assertUmRegistro(reserva, DONO_A, APROVADA, SOLICITADA, "alteração da reserva aprovada");
    }

    @Test
    @DisplayName("TC-07: o dono altera o horário de uma SOLICITADA: o estado não muda e nenhum registro é gravado")
    void alteracaoDeSolicitadaNaoGrava() {
        ReservaNaAgenda reserva = reservaEm(AGORA, SOLICITADA);

        assertAceita(cenario.altera(reserva, quarta(10, 0), quarta(11, 0), sala));

        assertThat(cenario.auditoria.todos()).isEmpty();
    }

    @Test
    @DisplayName("TC-08 (RN-09-T6): uma transição proibida é recusada e não grava registro")
    void transicaoProibidaNaoGrava() {
        ReservaNaAgenda reserva = reservaEm(AGORA, SOLICITADA);

        assertThat(cenario.ciclo.muda(reserva.id(), EM_USO, RESPONSAVEL).veredito()).isEqualTo(Veredito.RECUSADA);

        assertThat(cenario.auditoria.todos()).isEmpty();
    }

    @Test
    @DisplayName("TC-09 (RN-09-T7): um pedido recusado por conflito de horário não grava reserva nem registro")
    void pedidoRecusadoNaoGrava() {
        cenario = new CenarioDeReserva();
        sala = cenario.sala(102);
        cenario.reserva(DONO_B, APROVADA, terca(10, 0), terca(11, 0), sala);

        assertThat(cenario.cria(DONO_A, terca(10, 30), terca(11, 30), sala).veredito()).isEqualTo(Veredito.RECUSADA);

        assertThat(cenario.auditoria.todos()).isEmpty();
    }
}
