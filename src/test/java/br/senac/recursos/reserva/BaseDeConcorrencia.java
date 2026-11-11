package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static org.assertj.core.api.Assertions.assertThat;

import br.senac.recursos.seguranca.TokenService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Preparação comum dos testes de concorrência da T-37: banco limpo, relógio fixo, uma sala e a contagem pelo banco.
 *
 * <p>Para a evidência do vermelho (CA-13), rode com {@code -Ddupla-reserva.sem-protecao=true}: o EXCLUDE é removido
 * só do banco descartável do teste (Testcontainers), nunca por migration, e o contêiner some ao fim da execução.
 */
abstract class BaseDeConcorrencia {

    static final String SEM_PROTECAO = "dupla-reserva.sem-protecao";

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

    /** A disputa: quarta, 2026-11-11, das 10:00 às 12:00 UTC. */
    protected final Instant inicio = HorariosDeTeste.quarta(10, 0);
    protected final Instant fim = HorariosDeTeste.quarta(12, 0);

    @BeforeEach
    void prepararDisputa() throws Exception {
        ambiente = new AmbienteDeReserva(mvc, tokens, jdbc);
        ambiente.limpa();
        relogio.volta();
        if (Boolean.getBoolean(SEM_PROTECAO)) {
            jdbc.execute("ALTER TABLE reserva_recurso DROP CONSTRAINT IF EXISTS reserva_recurso_sem_sobreposicao");
        }
        sala = ambiente.sala("sala-102");
    }

    @AfterEach
    void limparDisputa() {
        ambiente.limpa();
        relogio.volta();
    }

    /** Uma tarefa de POST de reserva do usuário, devolvendo o status e o corpo da resposta. */
    protected Callable<DisparoSimultaneo.Resposta> pedido(int indice, String bearer, String json) {
        return () -> {
            var resposta = ambiente.cria(bearer, json).andReturn().getResponse();
            return new DisparoSimultaneo.Resposta(indice, resposta.getStatus(), resposta.getContentAsString());
        };
    }

    /** Todos os usuários pedem os mesmos recursos no mesmo intervalo, ao mesmo tempo. */
    protected List<DisparoSimultaneo.Resposta> disputa(int solicitantes, Instant ini, Instant fi,
            RecursoPedido... recursos) throws Exception {
        List<Long> ids = ambiente.novosSolicitantes(solicitantes);
        List<Callable<DisparoSimultaneo.Resposta>> tarefas = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            tarefas.add(pedido(i, ambiente.bearerDoUsuario(ids.get(i)), corpo(ini, fi, recursos)));
        }
        return DisparoSimultaneo.dispara(tarefas);
    }

    protected void assertUmaAceitaEOutrasRecusadas(List<DisparoSimultaneo.Resposta> respostas) {
        long aceitas = DisparoSimultaneo.quantosCom(respostas, 201);
        long recusadas = DisparoSimultaneo.quantosCom(respostas, 409);
        assertThat(aceitas).as("respostas 201").isEqualTo(1);
        assertThat(recusadas).as("respostas 409").isEqualTo(respostas.size() - 1);
    }
}
