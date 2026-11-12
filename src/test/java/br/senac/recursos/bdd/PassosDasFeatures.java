package br.senac.recursos.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.senac.recursos.sala.SalaRequest;
import br.senac.recursos.sala.SalaService;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.UsuarioRequest;
import br.senac.recursos.seguranca.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Os passos das features (T-36). Cada passo chama a API real por MockMvc, com o login e o token de cada perfil, como o
 * cliente faria: nada aqui conhece a regra de negócio, só observa a resposta e o banco. Os horários dos cenários são
 * da terça-feira 2026-11-10, em UTC, e o relógio fixo do teste está na segunda anterior, 08:00 UTC.
 */
public class PassosDasFeatures {

    private static final String SENHA = "senha-segura-123";
    private static final String SUFIXO = "@bdd.test";
    private static final String DIA = "2026-11-10";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private UsuarioService usuarios;
    @Autowired
    private SalaService salas;
    @Autowired
    private ObjectMapper json;

    private final Map<String, String> portadores = new HashMap<>();
    private final Map<String, Long> salasPorNome = new HashMap<>();
    private long reserva;
    private long manutencao;
    private long salaDaManutencao;
    private int status;
    private List<Integer> statusDosPedidos = List.of();

    @Before
    public void antes() {
        limpa();
    }

    @After
    public void depois() {
        limpa();
    }

    // ---------- Dado ----------

    @Dado("a sala {string}")
    public void aSala(String nome) {
        salasPorNome.put(nome, salas.cadastra(new SalaRequest(nome, "Laboratório", 30, "Bloco B", false)).getId());
    }

    @Dado("o solicitante {string}, o responsável {string} e o administrador {string}")
    public void osTresPerfis(String solicitante, String responsavel, String administrador) throws Exception {
        entra(solicitante, Perfil.SOLICITANTE);
        entra(responsavel, Perfil.RESPONSAVEL);
        entra(administrador, Perfil.ADMINISTRADOR);
    }

    @Dado("que {string} pediu a sala {string} na terça-feira das {word} às {word}")
    public void queAlguemPediu(String quem, String sala, String inicio, String fim) throws Exception {
        pede(quem, sala, inicio, fim);
        assertThat(status).as("o pedido de preparação do cenário deveria ser aceito").isEqualTo(201);
        reserva = json.readTree(ultimaResposta).get("id").asLong();
    }

    @Dado("que {string} aprovou a reserva")
    public void queAlguemAprovou(String quem) throws Exception {
        mudaEstado(quem, "APROVADA");
        assertThat(status).isEqualTo(200);
    }

    @Dado("que {string} rejeitou a reserva")
    public void queAlguemRejeitou(String quem) throws Exception {
        mudaEstado(quem, "REJEITADA");
        assertThat(status).isEqualTo(200);
    }

    @Dado("que a sala {string} está em manutenção na terça-feira das {word} às {word}")
    public void queASalaEstaEmManutencao(String sala, String inicio, String fim) throws Exception {
        registraManutencao("Admin", sala, inicio, fim);
        assertThat(status).isEqualTo(201);
        manutencao = json.readTree(ultimaResposta).get("id").asLong();
        salaDaManutencao = salasPorNome.get(sala);
    }

    @Dado("que {string} encerrou a manutenção da sala {string}")
    public void queAlguemEncerrouAManutencao(String quem, String sala) throws Exception {
        status = mvc.perform(delete("/api/recursos/" + salaDaManutencao + "/manutencoes/" + manutencao)
                .header(HttpHeaders.AUTHORIZATION, portadores.get(quem))).andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(204);
    }

    // ---------- Quando ----------

    @Quando("{string} aprova a reserva")
    public void aprova(String quem) throws Exception {
        mudaEstado(quem, "APROVADA");
    }

    @Quando("{string} rejeita a reserva")
    public void rejeita(String quem) throws Exception {
        mudaEstado(quem, "REJEITADA");
    }

    @Quando("{string} cancela a reserva")
    public void cancela(String quem) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/reservas/" + reserva + "/cancelamento")
                .header(HttpHeaders.AUTHORIZATION, portadores.get(quem))).andReturn();
        status = resultado.getResponse().getStatus();
    }

    @Quando("{string} pede a sala {string} na terça-feira das {word} às {word}")
    public void pede(String quem, String sala, String inicio, String fim) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/reservas").header(HttpHeaders.AUTHORIZATION, portadores.get(quem))
                .contentType(MediaType.APPLICATION_JSON).content(pedido(sala, inicio, fim))).andReturn();
        status = resultado.getResponse().getStatus();
        ultimaResposta = resultado.getResponse().getContentAsString();
    }

    @Quando("{string} registra manutenção da sala {string} na terça-feira das {word} às {word}")
    public void registraManutencao(String quem, String sala, String inicio, String fim) throws Exception {
        String corpo = "{\"inicio\":\"" + instante(inicio) + "\",\"termino\":\"" + instante(fim)
                + "\",\"motivo\":\"Manutenção do cenário\"}";
        MvcResult resultado = mvc.perform(post("/api/recursos/" + salasPorNome.get(sala) + "/manutencoes")
                .header(HttpHeaders.AUTHORIZATION, portadores.get(quem)).contentType(MediaType.APPLICATION_JSON)
                .content(corpo)).andReturn();
        status = resultado.getResponse().getStatus();
        ultimaResposta = resultado.getResponse().getContentAsString();
    }

    @Quando("{int} solicitantes pedem ao mesmo tempo a sala {string} na terça-feira das {word} às {word}")
    public void pedemAoMesmoTempo(int quantos, String sala, String inicio, String fim) throws Exception {
        List<String> portes = new ArrayList<>();
        for (int i = 1; i <= quantos; i++) {
            portes.add(entra("Solicitante" + i, Perfil.SOLICITANTE));
        }
        String corpo = pedido(sala, inicio, fim);
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(quantos);
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (String porte : portes) {
                Callable<Integer> disparo = () -> {
                    largada.await();
                    return mvc.perform(post("/api/reservas").header(HttpHeaders.AUTHORIZATION, porte)
                            .contentType(MediaType.APPLICATION_JSON).content(corpo)).andReturn().getResponse()
                            .getStatus();
                };
                futuros.add(pool.submit(disparo));
            }
            largada.countDown();
            List<Integer> resultados = new ArrayList<>();
            for (Future<Integer> futuro : futuros) {
                resultados.add(futuro.get());
            }
            statusDosPedidos = resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    // ---------- Então ----------

    @Então("a resposta é {int}")
    public void aRespostaE(int esperado) {
        assertThat(status).isEqualTo(esperado);
    }

    @Então("a reserva está {string}")
    public void aReservaEsta(String estado) {
        assertThat(jdbc.queryForList("SELECT estado FROM reserva WHERE id = ?", String.class, reserva))
                .containsExactly(estado);
    }

    @Então("o pedido é {word}")
    public void oPedidoE(String resultado) {
        assertThat(status).as("pedido " + resultado).isEqualTo(resultado.equals("aceito") ? 201 : 409);
    }

    @Então("exatamente {int} pedido é aceito e {int} é/são recusado(s)")
    public void exatamenteAceitosERecusados(int aceitos, int recusados) {
        assertThat(statusDosPedidos.stream().filter(s -> s == 201).count()).isEqualTo(aceitos);
        assertThat(statusDosPedidos.stream().filter(s -> s == 409).count()).isEqualTo(recusados);
        assertThat(statusDosPedidos).as("nenhum pedido pode terminar em erro inesperado")
                .allMatch(s -> s == 201 || s == 409);
    }

    @Então("existe {int} reserva que ocupa a sala")
    public void existeUmaReservaQueOcupaASala(int quantas) {
        long sala = salasPorNome.values().iterator().next();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva_recurso WHERE recurso_id = ? AND ocupa",
                Integer.class, sala)).isEqualTo(quantas);
    }

    @Então("o histórico da reserva tem os estados {string}")
    public void oHistoricoTemOsEstados(String esperados) throws Exception {
        MvcResult resultado = mvc.perform(get("/api/reservas/" + reserva + "/auditoria")
                .header(HttpHeaders.AUTHORIZATION, portadores.get("Carlos"))).andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        List<String> estados = new ArrayList<>();
        for (JsonNode registro : json.readTree(resultado.getResponse().getContentAsString())) {
            estados.add(registro.get("estadoNovo").asText());
        }
        assertThat(String.join(", ", estados)).isEqualTo(esperados);
    }

    // ---------- apoio ----------

    private String ultimaResposta = "";

    private void mudaEstado(String quem, String estado) throws Exception {
        MvcResult resultado = mvc.perform(patch("/api/reservas/" + reserva + "/estado")
                .header(HttpHeaders.AUTHORIZATION, portadores.get(quem)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"" + estado + "\"}")).andReturn();
        status = resultado.getResponse().getStatus();
    }

    private String pedido(String sala, String inicio, String fim) {
        return "{\"inicio\":\"" + instante(inicio) + "\",\"termino\":\"" + instante(fim)
                + "\",\"recursos\":[{\"tipo\":\"SALA\",\"id\":" + salasPorNome.get(sala) + "}]}";
    }

    /** "10:00" ou "10:00:00" na terça-feira do cenário, em UTC. */
    private static String instante(String hora) {
        return DIA + "T" + (hora.length() == 5 ? hora + ":00" : hora) + "Z";
    }

    /** Cria o usuário com o perfil, faz o login pela API e guarda o cabeçalho Authorization dele. */
    private String entra(String nome, Perfil perfil) throws Exception {
        String email = nome.toLowerCase().replace(" ", "") + SUFIXO;
        usuarios.cria(new UsuarioRequest(nome, email, SENHA, perfil));
        MvcResult login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}")).andReturn();
        assertThat(login.getResponse().getStatus()).isEqualTo(200);
        String portador = "Bearer " + json.readTree(login.getResponse().getContentAsString()).get("token").asText();
        portadores.put(nome, portador);
        return portador;
    }

    private void limpa() {
        jdbc.update("DELETE FROM conflito_evitado");
        jdbc.update("DELETE FROM retirada");
        // TRUNCATE não dispara o gatilho de imutabilidade (a auditoria só recebe inserções).
        jdbc.execute("TRUNCATE TABLE auditoria");
        jdbc.update("DELETE FROM periodo_manutencao");
        jdbc.update("DELETE FROM reserva_recurso");
        jdbc.update("DELETE FROM reserva");
        jdbc.update("DELETE FROM usuario WHERE email LIKE ?", "%" + SUFIXO);
        jdbc.update("DELETE FROM material");
        jdbc.update("DELETE FROM competencia");
        jdbc.update("DELETE FROM professor");
        jdbc.update("DELETE FROM sala");
        jdbc.update("DELETE FROM recurso");
    }
}
