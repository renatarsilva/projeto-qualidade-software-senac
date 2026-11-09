package br.senac.recursos.reserva;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import br.senac.recursos.seguranca.IdentidadesDeTeste;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.TokenService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.ServletException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Apoio dos testes de API da reserva: recursos, usuários, reservas gravadas direto no banco, chamadas e limpeza. */
final class AmbienteDeReserva {

    static final String EMAIL_DO_OUTRO = "outro-solicitante@exemplo.com";
    static final String NOME_DO_OUTRO = "Fulana Reservista";

    private final MockMvc mvc;
    private final TokenService tokens;
    private final JdbcTemplate jdbc;

    AmbienteDeReserva(MockMvc mvc, TokenService tokens, JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.tokens = tokens;
        this.jdbc = jdbc;
    }

    // Recursos, pela API do Administrador (já funciona desde a T-16, T-17 e T-18)

    RecursoPedido sala(String nome) throws Exception {
        String corpo = "{\"nome\":\"" + nome + "\",\"tipoSala\":\"Laboratório\",\"capacidade\":30,"
                + "\"localizacao\":\"Bloco B\",\"restrito\":false}";
        return new RecursoPedido(TipoDeRecurso.SALA, cadastra("/api/salas", corpo));
    }

    RecursoPedido professor(String nome) throws Exception {
        String corpo = "{\"nome\":\"" + nome + "\",\"restrito\":false,\"competencias\":[\"Java\"]}";
        return new RecursoPedido(TipoDeRecurso.PROFESSOR, cadastra("/api/professores", corpo));
    }

    RecursoPedido material(String nome, String patrimonio) throws Exception {
        String corpo = "{\"nome\":\"" + nome + "\",\"tipoMaterial\":\"Projetor\",\"patrimonio\":\"" + patrimonio
                + "\",\"restrito\":false}";
        return new RecursoPedido(TipoDeRecurso.MATERIAL, cadastra("/api/materiais", corpo));
    }

    private long cadastra(String caminho, String json) throws Exception {
        String resposta = executa(post(caminho)
                .header(HttpHeaders.AUTHORIZATION, IdentidadesDeTeste.bearerDe(tokens, Perfil.ADMINISTRADOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)).andReturn().getResponse().getContentAsString();
        return JsonPath.parse(resposta).read("$.id", Long.class);
    }

    // Usuários

    long solicitante() {
        return IdentidadesDeTeste.garanteUsuario(Perfil.SOLICITANTE);
    }

    String bearerDoSolicitante() {
        return IdentidadesDeTeste.bearerDoUsuario(tokens, solicitante(), Perfil.SOLICITANTE);
    }

    String bearerDe(Perfil perfil) {
        return IdentidadesDeTeste.bearerDe(tokens, perfil);
    }

    /** Segundo Solicitante: existe para ser "o outro usuário" (dono de outra reserva). */
    long outroSolicitante() {
        return jdbc.queryForObject(
                "INSERT INTO usuario (nome, email, senha_hash, perfil, ativo) VALUES (?, ?, 'hash-de-teste', 'SOLICITANTE', true) "
                        + "ON CONFLICT (email) DO UPDATE SET ativo = true RETURNING id",
                Long.class, NOME_DO_OUTRO, EMAIL_DO_OUTRO);
    }

    String bearerDoOutro() {
        return IdentidadesDeTeste.bearerDoUsuario(tokens, outroSolicitante(), Perfil.SOLICITANTE);
    }

    // Reservas gravadas direto no banco (montam o cenário sem passar pela API)

    long reservaGravada(long donoId, EstadoDaReserva estado, Instant inicio, Instant fim, Instant criadaEm,
            RecursoPedido... recursos) {
        boolean ocupa = estado == EstadoDaReserva.SOLICITADA || estado == EstadoDaReserva.APROVADA
                || estado == EstadoDaReserva.EM_USO;
        long id = jdbc.queryForObject(
                "INSERT INTO reserva (solicitante_id, inicio, fim, estado, criada_em, prazo_aprovacao_em) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class, donoId, utc(inicio), utc(fim), estado.name(), utc(criadaEm), utc(inicio.minusSeconds(86400)));
        for (RecursoPedido recurso : recursos) {
            jdbc.update("INSERT INTO reserva_recurso (reserva_id, recurso_id, periodo, ocupa) "
                    + "VALUES (?, ?, tstzrange(?::timestamptz, ?::timestamptz, '[)'), ?)",
                    id, recurso.id(), utc(inicio).toString(), utc(fim).toString(), ocupa);
        }
        return id;
    }

    int reservas() {
        return jdbc.queryForObject("SELECT count(*) FROM reserva", Integer.class);
    }

    int vinculos() {
        return jdbc.queryForObject("SELECT count(*) FROM reserva_recurso", Integer.class);
    }

    Instant inicioNoBanco(long reservaId) {
        return jdbc.queryForObject("SELECT inicio FROM reserva WHERE id = ?", OffsetDateTime.class, reservaId)
                .toInstant();
    }

    Instant fimNoBanco(long reservaId) {
        return jdbc.queryForObject("SELECT fim FROM reserva WHERE id = ?", OffsetDateTime.class, reservaId)
                .toInstant();
    }

    Instant criadaEmNoBanco(long reservaId) {
        return jdbc.queryForObject("SELECT criada_em FROM reserva WHERE id = ?", OffsetDateTime.class, reservaId)
                .toInstant();
    }

    String estadoNoBanco(long reservaId) {
        return jdbc.queryForObject("SELECT estado FROM reserva WHERE id = ?", String.class, reservaId);
    }

    /** Ids dos recursos ligados à reserva, em ordem. */
    List<Long> recursosNoBanco(long reservaId) {
        return jdbc.queryForList("SELECT recurso_id FROM reserva_recurso WHERE reserva_id = ? ORDER BY recurso_id",
                Long.class, reservaId);
    }

    long ultimaReserva() {
        return jdbc.queryForObject("SELECT max(id) FROM reserva", Long.class);
    }

    long donoNoBanco(long reservaId) {
        return jdbc.queryForObject("SELECT solicitante_id FROM reserva WHERE id = ?", Long.class, reservaId);
    }

    // Chamadas

    static String corpo(Instant inicio, Instant termino, RecursoPedido... recursos) {
        return corpoComExtras(inicio, termino, "", recursos);
    }

    /** {@code extras} entra no JSON como foi escrito (por exemplo, {@code "criadaEm":"..."}). */
    static String corpoComExtras(Instant inicio, Instant termino, String extras, RecursoPedido... recursos) {
        String lista = Arrays.stream(recursos)
                .map(r -> "{\"tipo\":\"" + r.tipo() + "\",\"id\":" + r.id() + "}")
                .collect(Collectors.joining(","));
        StringBuilder json = new StringBuilder("{");
        if (inicio != null) {
            json.append("\"inicio\":\"").append(inicio).append("\",");
        }
        if (termino != null) {
            json.append("\"termino\":\"").append(termino).append("\",");
        }
        if (!extras.isEmpty()) {
            json.append(extras).append(",");
        }
        return json.append("\"recursos\":[").append(lista).append("]}").toString();
    }

    ResultActions cria(String bearer, String json) throws Exception {
        MockHttpServletRequestBuilder requisicao = post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON).content(json);
        return executa(comAutorizacao(requisicao, bearer));
    }

    ResultActions altera(String bearer, long id, String json) throws Exception {
        MockHttpServletRequestBuilder requisicao = put("/api/reservas/" + id)
                .contentType(MediaType.APPLICATION_JSON).content(json);
        return executa(comAutorizacao(requisicao, bearer));
    }

    private static MockHttpServletRequestBuilder comAutorizacao(MockHttpServletRequestBuilder requisicao,
            String bearer) {
        return bearer == null ? requisicao : requisicao.header(HttpHeaders.AUTHORIZATION, bearer);
    }

    /** Uma exceção que escapa da API é resposta errada (deveria ser 4xx): vira falha de asserção, e não erro. */
    private ResultActions executa(MockHttpServletRequestBuilder requisicao) throws Exception {
        try {
            return mvc.perform(requisicao);
        } catch (ServletException erro) {
            throw new AssertionError("A API deixou escapar uma exceção em vez de responder com erro 4xx: "
                    + erro.getMessage(), erro);
        }
    }

    void limpa() {
        jdbc.update("DELETE FROM reserva_recurso");
        jdbc.update("DELETE FROM reserva");
        jdbc.update("DELETE FROM usuario WHERE email = ?", EMAIL_DO_OUTRO);
        jdbc.update("DELETE FROM material");
        jdbc.update("DELETE FROM competencia");
        jdbc.update("DELETE FROM professor");
        jdbc.update("DELETE FROM sala");
        jdbc.update("DELETE FROM recurso");
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
