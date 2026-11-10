package br.senac.recursos.reserva;

import br.senac.recursos.seguranca.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Preparação comum dos testes de API do ciclo de vida: banco limpo, relógio fixo, uma sala e o dono das reservas. */
abstract class BaseDaApiDeReserva {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected TokenService tokens;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected RelogioDeTeste relogio;

    protected AmbienteDeReserva ambiente;
    protected RecursoPedido sala;
    protected long dono;

    @BeforeEach
    void prepararAmbiente() throws Exception {
        ambiente = new AmbienteDeReserva(mvc, tokens, jdbc);
        ambiente.limpa();
        relogio.volta();
        sala = ambiente.sala("sala-102");
        dono = ambiente.solicitante();
    }

    @AfterEach
    void limparAmbiente() {
        ambiente.limpa();
        relogio.volta();
    }

    /** Reserva do dono, terça das 10:00 às 11:00, no estado pedido (prazo de aprovação: segunda, 10:00). */
    protected long reservaDoDono(EstadoDaReserva estado) {
        return ambiente.reservaGravada(dono, estado, HorariosDeTeste.terca(10, 0), HorariosDeTeste.terca(11, 0),
                HorariosDeTeste.AGORA, sala);
    }
}
