package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static br.senac.recursos.reserva.HorariosDeTeste.AGORA;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.seguranca.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** A garantia do PostgreSQL contra sobreposição e a tradução para 409 (spec T-20, CA-04; TC-57 e TC-58). */
@TesteDeReservaApi
class ConflitoNoBancoDeReservaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RelogioDeTeste relogio;

    /** Contorna a verificação da aplicação (TC-58): o mock nunca vê conflito, então só o banco pode recusar. */
    @MockitoBean
    private VerificadorDeSobreposicao verificador;

    private AmbienteDeReserva ambiente;
    private RecursoPedido sala;

    @BeforeEach
    void preparar() throws Exception {
        ambiente = new AmbienteDeReserva(mvc, tokens, jdbc);
        ambiente.limpa();
        relogio.volta();
        when(verificador.ocupado(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(false);
        sala = ambiente.sala("sala-102");
    }

    @AfterEach
    void limpar() {
        ambiente.limpa();
        relogio.volta();
    }

    @Test
    @DisplayName("TC-57: duas reservas ativas sobrepostas no mesmo recurso, inseridas direto, são recusadas pelo banco")
    void bancoRecusaSobreposicaoDeReservasAtivas() {
        ambiente.reservaGravada(ambiente.solicitante(), EstadoDaReserva.SOLICITADA, terca(10, 0), terca(11, 0),
                AGORA, sala);

        assertThatThrownBy(() -> ambiente.reservaGravada(ambiente.outroSolicitante(), EstadoDaReserva.SOLICITADA,
                terca(10, 30), terca(11, 30), AGORA, sala))
                .as("o EXCLUDE deve recusar a segunda reserva ativa")
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("TC-58: se a aplicação não vê o conflito, a recusa do banco vira 409 com a mensagem fixa e nada é gravado")
    void recusaDoBancoViraConflito() throws Exception {
        ambiente.reservaGravada(ambiente.outroSolicitante(), EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0),
                AGORA, sala);

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(10, 30), terca(11, 30), sala))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta operação conflita com outro registro"));
        assertThat(ambiente.reservas()).as("nenhuma linha nova em reserva").isEqualTo(1);
        assertThat(ambiente.vinculos()).as("nenhuma linha nova em reserva_recurso").isEqualTo(1);
    }
}
