package br.senac.recursos.notificacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.senac.recursos.TesteDeApiComBanco;
import br.senac.recursos.notificacao.ResultadoDaNotificacao.Situacao;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Sem url-base, a aplicação usa o modo simulado e sobe sem a API externa (spec T-31, CA-09; TC-13). */
@TesteDeApiComBanco
@ExtendWith(OutputCaptureExtension.class)
class NotificadorSimuladoTest {

    @Autowired
    private Notificador notificador;

    @Test
    @DisplayName("TC-13: sem app.notificacao.url-base o Notificador é o NotificadorEmLog: devolve ENVIADA e registra 1 aviso no log")
    void modoSimulado(CapturedOutput saida) {
        assertThat(notificador).isInstanceOf(NotificadorEmLog.class);

        ResultadoDaNotificacao resultado = notificador.notifica(new Notificacao(7, EventoDeNotificacao.CANCELADA,
                "marina@senac.br", Instant.parse("2026-11-12T12:00:00Z")));

        assertThat(resultado.situacao()).isEqualTo(Situacao.ENVIADA);
        assertThat(saida.getAll().lines().filter(l -> l.contains("Notificação simulada") && l.contains("reserva 7")))
                .hasSize(1);
    }
}
