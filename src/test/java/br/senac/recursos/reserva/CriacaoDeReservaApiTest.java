package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static br.senac.recursos.reserva.AmbienteDeReserva.corpoComExtras;
import static br.senac.recursos.reserva.HorariosDeTeste.AGORA;
import static br.senac.recursos.reserva.HorariosDeTeste.segunda;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.seguranca.TokenService;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Contrato do POST /api/reservas (spec T-20, seção 4; TC-29 a TC-31, TC-38, TC-43, TC-44, TC-47 a TC-49, TC-55, TC-56). */
@TesteDeReservaApi
class CriacaoDeReservaApiTest {

    private static final String ANTECEDENCIA = "A reserva precisa ser pedida com pelo menos 1 hora de antecedência";
    private static final String SALA_OCUPADA = "A sala já está reservada neste horário";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RelogioDeTeste relogio;

    private AmbienteDeReserva ambiente;
    private RecursoPedido sala;

    @BeforeEach
    void preparar() throws Exception {
        ambiente = new AmbienteDeReserva(mvc, tokens, jdbc);
        ambiente.limpa();
        relogio.volta();
        sala = ambiente.sala("sala-102");
    }

    @AfterEach
    void limpar() {
        ambiente.limpa();
        relogio.volta();
    }

    @Test
    @DisplayName("TC-29: término igual ao início devolve 400 com o erro no campo termino")
    void terminoIgualAoInicioDevolve400() throws Exception {
        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(10, 0), terca(10, 0), sala))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("termino"))
                .andExpect(jsonPath("$.erros[0].motivo").value("deve ser posterior ao início"));
        assertThat(ambiente.reservas()).isZero();
    }

    @Test
    @DisplayName("TC-30: pedido sobreposto devolve 409 com a mensagem de negócio e não grava nada")
    void pedidoSobrepostoDevolve409() throws Exception {
        ambiente.reservaGravada(ambiente.outroSolicitante(), EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0),
                AGORA, sala);

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(10, 30), terca(11, 30), sala))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(SALA_OCUPADA));
        assertThat(ambiente.reservas()).as("linhas em reserva").isEqualTo(1);
        assertThat(ambiente.vinculos()).as("linhas em reserva_recurso").isEqualTo(1);
    }

    @Test
    @DisplayName("TC-31: intervalo livre devolve 201 com a reserva SOLICITADA")
    void intervaloLivreDevolve201() throws Exception {
        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(10, 0), terca(11, 0), sala))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("SOLICITADA"));
        assertThat(ambiente.reservas()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-38 (RN-01-T9): recurso inexistente devolve 404 e nada é gravado")
    void recursoInexistenteDevolve404() throws Exception {
        RecursoPedido inexistente = new RecursoPedido(TipoDeRecurso.SALA, 999999);

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(10, 0), terca(11, 0), inexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Recurso não encontrado"));
        assertThat(ambiente.reservas()).isZero();
    }

    @Test
    @DisplayName("TC-43: um solicitanteId de outro usuário no corpo é ignorado; o dono é o autenticado")
    void donoVemDoUsuarioAutenticado() throws Exception {
        long outro = ambiente.outroSolicitante();
        String json = corpoComExtras(terca(10, 0), terca(11, 0), "\"solicitanteId\":" + outro, sala);

        ambiente.cria(ambiente.bearerDoSolicitante(), json).andExpect(status().isCreated());

        assertThat(ambiente.donoNoBanco(ambiente.ultimaReserva())).isEqualTo(ambiente.solicitante());
    }

    @Test
    @DisplayName("TC-44: o 409 cita o recurso e não revela quem reservou")
    void conflitoNaoRevelaOOutroUsuario() throws Exception {
        long outro = ambiente.outroSolicitante();
        ambiente.reservaGravada(outro, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), AGORA, sala);

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(10, 0), terca(11, 0), sala))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("sala")))
                .andExpect(jsonPath("$.detail").value(not(containsString(AmbienteDeReserva.NOME_DO_OUTRO))))
                .andExpect(jsonPath("$.detail").value(not(containsString(AmbienteDeReserva.EMAIL_DO_OUTRO))));
    }

    @Test
    @DisplayName("TC-47 (RN-15-T2 e T10): início 30 min depois do Clock devolve 409 de antecedência e não grava")
    void antecedenciaInsuficienteDevolve409() throws Exception {
        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(segunda(8, 30), segunda(9, 30), sala))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(ANTECEDENCIA));
        assertThat(ambiente.reservas()).isZero();
        assertThat(ambiente.vinculos()).isZero();
    }

    @Test
    @DisplayName("TC-48: a reserva persistida tem criada_em igual ao instante do Clock")
    void criadaEmVemDoClock() throws Exception {
        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(10, 0), terca(11, 0), sala))
                .andExpect(status().isCreated());

        assertThat(ambiente.criadaEmNoBanco(ambiente.ultimaReserva())).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("TC-49 (RN-15-T8): criadaEm e prazoAprovacaoEm enviados no corpo são ignorados")
    void camposCalculadosDoCorpoSaoIgnorados() throws Exception {
        String extras = "\"criadaEm\":\"2020-01-01T00:00:00Z\",\"prazoAprovacaoEm\":\"2020-01-02T00:00:00Z\"";

        ambiente.cria(ambiente.bearerDoSolicitante(),
                corpoComExtras(terca(10, 0), terca(11, 0), extras, sala)).andExpect(status().isCreated());

        long id = ambiente.ultimaReserva();
        assertThat(ambiente.criadaEmNoBanco(id)).isEqualTo(AGORA);
        Instant prazo = jdbc.queryForObject("SELECT prazo_aprovacao_em FROM reserva WHERE id = ?",
                java.time.OffsetDateTime.class, id).toInstant();
        assertThat(prazo).isNotEqualTo(Instant.parse("2020-01-02T00:00:00Z"));
    }

    @Test
    @DisplayName("TC-55 (RN-15-T7): sem antecedência e com a sala ocupada, a resposta é a de antecedência")
    void antecedenciaVemAntesDaSobreposicao() throws Exception {
        ambiente.reservaGravada(ambiente.outroSolicitante(), EstadoDaReserva.APROVADA, segunda(8, 30), segunda(9, 30),
                AGORA, sala);

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(segunda(8, 30), segunda(9, 30), sala))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(ANTECEDENCIA))
                .andExpect(jsonPath("$.detail").value(not(containsString("já está reservada"))));
        assertThat(ambiente.reservas()).as("nenhuma linha nova em reserva").isEqualTo(1);
        assertThat(ambiente.vinculos()).as("nenhuma linha nova em reserva_recurso").isEqualTo(1);
    }

    @Test
    @DisplayName("TC-56: com antecedência suficiente e a sala ocupada, a mensagem é a de sobreposição")
    void sobreposicaoTemMensagemPropria() throws Exception {
        ambiente.reservaGravada(ambiente.outroSolicitante(), EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0),
                AGORA, sala);

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(10, 0), terca(11, 0), sala))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(SALA_OCUPADA))
                .andExpect(jsonPath("$.detail").value(not(containsString("antecedência"))));
    }
}
