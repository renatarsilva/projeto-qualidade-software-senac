package br.senac.recursos.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import br.senac.recursos.TestcontainersConfiguration;
import br.senac.recursos.sala.SalaRequest;
import br.senac.recursos.sala.SalaService;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.UsuarioRequest;
import br.senac.recursos.seguranca.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Teste ponta a ponta (spec T-44 e T-47): a aplicação sobe num servidor de verdade, numa porta real, com o PostgreSQL
 * real, e o teste fala com ela por HTTP, com cookie de sessão, redirecionamentos e token CSRF, como um navegador faria.
 * Diferente do MockMvc, aqui o {@code sendError} do Spring Security é encaminhado para {@code /error}: foi assim que
 * o DEF-04 (403 virando 500) apareceu, e este teste o teria pego no CI. Usa o relógio de verdade, então as datas são
 * sempre daqui a alguns dias.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.seguranca.chave-assinatura=chave-de-teste-com-pelo-menos-32-caracteres-123",
        "app.admin-inicial.email=admin-de-teste@exemplo.com",
        "app.admin-inicial.senha=senha-inicial-de-teste"})
@DisplayName("Ponta a ponta: servidor de verdade, HTTP, sessão e CSRF")
class FluxoPorHttpRealTest {

    private static final String SENHA = "senha-segura-123";
    private static final String SUFIXO = "@e2e.test";
    private static final Pattern TOKEN_CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    @LocalServerPort
    private int porta;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private UsuarioService usuarios;
    @Autowired
    private SalaService salas;
    @Autowired
    private ObjectMapper json;

    private long sala;

    @BeforeEach
    void preparar() {
        limpa();
        for (Perfil perfil : Perfil.values()) {
            usuarios.cria(new UsuarioRequest("Pessoa " + perfil, perfil.name().toLowerCase() + SUFIXO, SENHA, perfil));
        }
        usuarios.cria(new UsuarioRequest("Rafael", "rafael" + SUFIXO, SENHA, Perfil.SOLICITANTE));
        sala = salas.cadastra(new SalaRequest("sala-102", "Laboratório", 30, "Bloco B", false)).getId();
    }

    @AfterEach
    void limpar() {
        limpa();
    }

    @Test
    @DisplayName("Anônimo vai para o login; no perfil errado e sem CSRF a resposta é 403 com a página amigável, e não 500")
    void acessoNegadoNoServidorDeVerdade() throws Exception {
        Navegador anonimo = new Navegador();
        Resposta pedido = anonimo.get("/reservas");
        assertThat(pedido.status()).isEqualTo(302);
        assertThat(pedido.local()).endsWith("/login");

        Navegador marina = entra("solicitante");
        Resposta negado = marina.get("/admin/salas");
        assertThat(negado.status()).as("perfil errado nas telas").isEqualTo(403);
        assertThat(negado.corpo()).contains("Acesso negado").doesNotContain("Exception");

        Resposta semToken = marina.postForm("/reservas", Map.of("inicio", "2030-01-01T10:00",
                "termino", "2030-01-01T11:00", "recursos", "SALA:" + sala), false);
        assertThat(semToken.status()).as("POST sem CSRF").isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva", Integer.class)).isZero();

        assertThat(marina.get("/rota-que-nao-existe").status()).as("rota sem regra").isEqualTo(403);
    }

    @Test
    @DisplayName("Fluxo completo pelas telas: reservar, conflito, aprovar e cancelar, com auditoria")
    void fluxoCompletoPelasTelas() throws Exception {
        Navegador marina = entra("solicitante");
        Navegador rafael = entra("rafael");
        Navegador carlos = entra("responsavel");
        String inicio = amanhaMais(3) + "T10:00";
        String termino = amanhaMais(3) + "T11:00";

        Resposta criada = marina.postForm("/reservas",
                Map.of("inicio", inicio, "termino", termino, "recursos", "SALA:" + sala), true);
        assertThat(criada.status()).isEqualTo(302);
        long reserva = Long.parseLong(criada.local().substring(criada.local().lastIndexOf('/') + 1));
        assertThat(marina.get("/reservas/" + reserva).corpo()).contains("Solicitada");

        Resposta conflito = rafael.postForm("/reservas", Map.of("inicio", amanhaMais(3) + "T10:30",
                "termino", amanhaMais(3) + "T11:30", "recursos", "SALA:" + sala), true);
        assertThat(conflito.status()).isEqualTo(200);
        assertThat(conflito.corpo()).contains("já está reservada").doesNotContain("Exception");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva", Integer.class)).isEqualTo(1);

        assertThat(marina.postForm("/reservas/" + reserva + "/aprovar", Map.of(), true).status())
                .as("o Solicitante não aprova").isEqualTo(403);
        assertThat(carlos.postForm("/reservas/" + reserva + "/aprovar", Map.of(), true).status()).isEqualTo(302);
        assertThat(estadoDa(reserva)).isEqualTo("APROVADA");

        assertThat(marina.postForm("/reservas/" + reserva + "/cancelar", Map.of(), true).status()).isEqualTo(302);
        assertThat(estadoDa(reserva)).isEqualTo("CANCELADA");
        assertThat(carlos.get("/reservas/" + reserva).corpo()).contains("Histórico de mudanças");
        assertThat(jdbc.queryForList("SELECT estado_novo FROM auditoria WHERE reserva_id = ? ORDER BY id",
                String.class, reserva)).containsExactly("SOLICITADA", "APROVADA", "CANCELADA");
    }

    @Test
    @DisplayName("API no servidor de verdade: 401 sem token, login, token Bearer e documentação pública")
    void apiNoServidorDeVerdade() throws Exception {
        Navegador cliente = new Navegador();
        Resposta semToken = cliente.get("/api/salas");
        assertThat(semToken.status()).isEqualTo(401);
        assertThat(semToken.corpo()).contains("\"status\":401");

        Resposta login = cliente.postJson("/api/auth/login",
                "{\"email\":\"administrador" + SUFIXO + "\",\"senha\":\"" + SENHA + "\"}", null);
        assertThat(login.status()).isEqualTo(200);
        String token = json.readTree(login.corpo()).get("token").asText();
        Resposta lista = cliente.get("/api/salas", token);
        assertThat(lista.status()).isEqualTo(200);
        assertThat(lista.corpo()).contains("sala-102");

        assertThat(cliente.get("/swagger-ui/index.html").status()).isEqualTo(200);
        JsonNode docs = json.readTree(cliente.get("/v3/api-docs").corpo());
        assertThat(docs.has("paths")).isTrue();
    }

    // ---------- apoio ----------

    private Navegador entra(String apelido) throws Exception {
        Navegador navegador = new Navegador();
        Resposta pagina = navegador.get("/login");
        Matcher token = TOKEN_CSRF.matcher(pagina.corpo());
        assertThat(token.find()).as("o formulário de login traz o token CSRF").isTrue();
        Resposta login = navegador.postForm("/login", Map.of("username", apelido + SUFIXO, "password", SENHA,
                "_csrf", token.group(1)), false);
        assertThat(login.status()).isEqualTo(302);
        assertThat(login.local()).as("login válido vai para a página inicial").doesNotContain("erro");
        // Depois do login a sessão é outra e o token também: o navegador lê o novo ao abrir a próxima página.
        navegador.tokenCsrf = null;
        return navegador;
    }

    private String estadoDa(long reserva) {
        return jdbc.queryForObject("SELECT estado FROM reserva WHERE id = ?", String.class, reserva);
    }

    private static String amanhaMais(int dias) {
        return LocalDate.now(BRASILIA).plusDays(dias).toString();
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

    record Resposta(int status, String local, String corpo) {
    }

    /** Um "navegador": guarda os cookies da sessão e não segue redirecionamentos sozinho, para o teste vê-los. */
    private final class Navegador {

        private final HttpClient cliente = HttpClient.newBuilder().cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER).build();
        private String tokenCsrf;

        Resposta get(String caminho) throws IOException, InterruptedException {
            return get(caminho, null);
        }

        Resposta get(String caminho, String bearer) throws IOException, InterruptedException {
            HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create(url(caminho))).GET();
            if (bearer != null) {
                pedido.header("Authorization", "Bearer " + bearer);
            }
            Resposta resposta = envia(pedido.build());
            Matcher csrf = TOKEN_CSRF.matcher(resposta.corpo());
            if (csrf.find()) {
                tokenCsrf = csrf.group(1);
            }
            return resposta;
        }

        /** Envia um formulário. Com {@code comToken}, junta o token CSRF lido da última página aberta. */
        Resposta postForm(String caminho, Map<String, String> campos, boolean comToken)
                throws IOException, InterruptedException {
            Map<String, String> corpo = new LinkedHashMap<>(campos);
            if (comToken) {
                if (tokenCsrf == null) {
                    get("/");
                }
                corpo.put("_csrf", tokenCsrf);
            }
            String formulario = corpo.entrySet().stream()
                    .map(e -> codifica(e.getKey()) + "=" + codifica(e.getValue())).collect(Collectors.joining("&"));
            return envia(HttpRequest.newBuilder(URI.create(url(caminho)))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formulario)).build());
        }

        Resposta postJson(String caminho, String corpo, String bearer) throws IOException, InterruptedException {
            HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create(url(caminho)))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(corpo));
            if (bearer != null) {
                pedido.header("Authorization", "Bearer " + bearer);
            }
            return envia(pedido.build());
        }

        private Resposta envia(HttpRequest pedido) throws IOException, InterruptedException {
            HttpResponse<String> resposta = cliente.send(pedido, HttpResponse.BodyHandlers.ofString());
            return new Resposta(resposta.statusCode(), resposta.headers().firstValue("Location").orElse(""),
                    resposta.body());
        }

        private String url(String caminho) {
            return "http://localhost:" + porta + caminho;
        }

        private String codifica(String texto) {
            return URLEncoder.encode(texto, StandardCharsets.UTF_8);
        }
    }
}
