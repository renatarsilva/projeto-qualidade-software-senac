package br.senac.recursos.notificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.senac.recursos.TesteDeApiComBanco;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** A notificação só sai depois do commit, e a falha dela não desfaz nada (spec T-31, CA-10; TC-16; RNF-10). */
@TesteDeApiComBanco
class NotificacaoAposOCommitTest {

    @MockitoBean
    private Notificador notificador;

    @Autowired
    private ApplicationEventPublisher publicador;

    @Autowired
    private PlatformTransactionManager transacoes;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void criaATabelaDeTeste() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS teste_notificacao (id INT)");
        jdbc.update("DELETE FROM teste_notificacao");
    }

    @AfterEach
    void apagaATabelaDeTeste() {
        jdbc.execute("DROP TABLE IF EXISTS teste_notificacao");
    }

    private ReservaNotificavel evento() {
        return new ReservaNotificavel(3, EventoDeNotificacao.APROVADA, "marina@senac.br",
                Instant.parse("2026-11-12T12:00:00Z"));
    }

    @Test
    @DisplayName("TC-16: transação confirmada com a notificação falhando: o registro continua gravado e o notificador é chamado 1 vez")
    void falhaDaNotificacaoNaoDesfazOCommit() {
        when(notificador.notifica(any())).thenReturn(ResultadoDaNotificacao.falhou(CausaDeFalha.TIMEOUT));

        new TransactionTemplate(transacoes).executeWithoutResult(status -> {
            jdbc.update("INSERT INTO teste_notificacao (id) VALUES (1)");
            publicador.publishEvent(evento());
            verify(notificador, never()).notifica(any());
        });

        assertThat(jdbc.queryForObject("SELECT count(*) FROM teste_notificacao", Integer.class)).isEqualTo(1);
        verify(notificador, times(1)).notifica(any());
    }

    @Test
    @DisplayName("TC-16: transação desfeita: nenhuma notificação é enviada")
    void transacaoDesfeitaNaoNotifica() {
        new TransactionTemplate(transacoes).executeWithoutResult(status -> {
            jdbc.update("INSERT INTO teste_notificacao (id) VALUES (2)");
            publicador.publishEvent(evento());
            status.setRollbackOnly();
        });

        assertThat(jdbc.queryForObject("SELECT count(*) FROM teste_notificacao", Integer.class)).isZero();
        verify(notificador, never()).notifica(any());
    }
}
