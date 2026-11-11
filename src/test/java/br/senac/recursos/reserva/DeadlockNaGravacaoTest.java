package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static br.senac.recursos.reserva.HorariosDeTeste.quarta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import br.senac.recursos.seguranca.TokenService;

/** Deadlock abortado pelo banco (40P01) vira 409 genérico, sem gravação (spec T-37, CA-14; TC-18). */
@TesteDeReservaApi
class DeadlockNaGravacaoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RelogioDeTeste relogio;

    @MockitoSpyBean
    private AgendaNoBanco agenda;

    private AmbienteDeReserva ambiente;

    @BeforeEach
    void preparar() {
        ambiente = new AmbienteDeReserva(mvc, tokens, jdbc);
        ambiente.limpa();
        relogio.volta();
    }

    @AfterEach
    void limpar() {
        ambiente.limpa();
        relogio.volta();
    }

    @Test
    @DisplayName("TC-18: o banco aborta a transação por deadlock (40P01): 409 com a mensagem genérica e nada gravado")
    void deadlockViraConflito() throws Exception {
        RecursoPedido sala = ambiente.sala("sala-102");
        doThrow(new DeadlockLoserDataAccessException("deadlock detectado", new SQLException("deadlock", "40P01")))
                .when(agenda).grava(any());

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(quarta(10, 0), quarta(12, 0), sala))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta operação conflita com outro registro"));
        assertThat(ambiente.reservas()).as("linhas em reserva").isZero();
        assertThat(ambiente.vinculos()).as("linhas em reserva_recurso").isZero();
    }
}
