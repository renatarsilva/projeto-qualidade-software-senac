package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.notificacao.CausaDeFalha;
import br.senac.recursos.notificacao.EventoDeNotificacao;
import br.senac.recursos.notificacao.Notificacao;
import br.senac.recursos.notificacao.Notificador;
import br.senac.recursos.notificacao.ResultadoDaNotificacao;
import br.senac.recursos.seguranca.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * A aprovação, a rejeição e o cancelamento avisam o dono da reserva, e a falha do aviso não desfaz a mudança
 * (spec T-31 e RF-11; CT-03 e RNF-10). Check-in não notifica (DP-03).
 */
@TesteDeReservaApi
class NotificacaoDasMudancasApiTest extends BaseDaApiDeReserva {

    @MockitoBean
    private Notificador notificador;

    private Notificacao notificacaoEnviada() {
        ArgumentCaptor<Notificacao> captura = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificador, times(1)).notifica(captura.capture());
        return captura.getValue();
    }

    @Test
    @DisplayName("A aprovação pelo Responsável notifica o dono: 1 aviso APROVADA, com a reserva e o e-mail do dono")
    void aprovacaoNotifica() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);
        when(notificador.notifica(any())).thenReturn(ResultadoDaNotificacao.enviada("n-1"));

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, "APROVADA").andExpect(status().isOk());

        Notificacao aviso = notificacaoEnviada();
        assertThat(aviso.evento()).isEqualTo(EventoDeNotificacao.APROVADA);
        assertThat(aviso.reservaId()).isEqualTo(id);
        assertThat(aviso.destinatario()).isEqualTo("teste-solicitante@exemplo.com");
    }

    @Test
    @DisplayName("A rejeição pelo Responsável notifica o dono com o evento REJEITADA")
    void rejeicaoNotifica() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);
        when(notificador.notifica(any())).thenReturn(ResultadoDaNotificacao.enviada("n-1"));

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, "REJEITADA").andExpect(status().isOk());

        assertThat(notificacaoEnviada().evento()).isEqualTo(EventoDeNotificacao.REJEITADA);
    }

    @Test
    @DisplayName("O cancelamento pelo dono, antes do início, notifica com o evento CANCELADA")
    void cancelamentoNotifica() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.APROVADA);
        when(notificador.notifica(any())).thenReturn(ResultadoDaNotificacao.enviada("n-1"));

        ambiente.cancela(ambiente.bearerDoSolicitante(), id).andExpect(status().isOk());

        assertThat(notificacaoEnviada().evento()).isEqualTo(EventoDeNotificacao.CANCELADA);
    }

    @Test
    @DisplayName("O check-in não notifica (só aprovação, rejeição e cancelamento, DP-03)")
    void checkinNaoNotifica() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.APROVADA);
        relogio.defineAgora(terca(10, 30));

        ambiente.checkin(ambiente.bearerDoSolicitante(), id).andExpect(status().isOk());

        verify(notificador, never()).notifica(any());
    }

    @Test
    @DisplayName("CT-03: a notificação falha ao aprovar e a aprovação é preservada: 200 e a reserva continua APROVADA")
    void falhaDaNotificacaoPreservaAAprovacao() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);
        when(notificador.notifica(any())).thenReturn(ResultadoDaNotificacao.falhou(CausaDeFalha.TIMEOUT));

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, "APROVADA").andExpect(status().isOk());

        assertThat(ambiente.estadoNoBanco(id)).isEqualTo("APROVADA");
        verify(notificador, times(1)).notifica(any());
    }

    @Test
    @DisplayName("Uma mudança recusada não notifica")
    void recusaNaoNotifica() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.CANCELADA);

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, "APROVADA").andExpect(status().isConflict());

        verify(notificador, never()).notifica(any());
    }
}
