package br.senac.recursos.sala;

import static br.senac.recursos.sala.SalasDeTeste.cadastra;
import static br.senac.recursos.sala.SalasDeTeste.corpoValido;
import static br.senac.recursos.sala.SalasDeTeste.limpaBanco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TesteDeApiComBanco;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.TokenService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Nome único da sala (spec T-16, CA-04; casos TC-10 e TC-38).
 */
@TesteDeApiComBanco
class NomeUnicoDaSalaApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private SalaRepository salas;

    @BeforeEach
    void limpar() {
        limpaBanco(jdbc);
    }

    private long totalDeSalas() {
        return jdbc.queryForObject("SELECT count(*) FROM sala", Long.class);
    }

    @Test
    void tc10_cadastrarOMesmoNomeDuasVezesDa409ComUmaSoSalaGravada() throws Exception {
        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpoValido("sala-01")).andExpect(status().isCreated());

        cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpoValido("sala-01"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe uma sala com esse nome"));

        assertThat(totalDeSalas()).isEqualTo(1);
    }

    @Test
    void tc38_doisCadastrosSimultaneosComAVerificacaoDoServicoBurladaDao201E409ComMensagemFixa() throws Exception {
        // A verificação do serviço diz "não existe" para os dois pedidos: só o índice único do banco decide.
        doReturn(false).when(salas).existsByNomeIgnoreCase(anyString());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<MvcResult>> chamadas = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Callable<MvcResult> chamada = () -> {
                largada.await();
                return cadastra(mvc, tokens, Perfil.ADMINISTRADOR, corpoValido("sala-05")).andReturn();
            };
            chamadas.add(pool.submit(chamada));
        }
        largada.countDown();

        List<Integer> statusDasRespostas = new ArrayList<>();
        String corpoDoConflito = null;
        for (Future<MvcResult> chamada : chamadas) {
            MvcResult resultado = chamada.get();
            statusDasRespostas.add(resultado.getResponse().getStatus());
            if (resultado.getResponse().getStatus() == 409) {
                corpoDoConflito = resultado.getResponse().getContentAsString();
            }
        }
        pool.shutdown();

        assertThat(statusDasRespostas).containsExactlyInAnyOrder(201, 409);
        assertThat(corpoDoConflito).contains("Esta operação conflita com outro registro");
        assertThat(totalDeSalas()).isEqualTo(1);
    }
}
