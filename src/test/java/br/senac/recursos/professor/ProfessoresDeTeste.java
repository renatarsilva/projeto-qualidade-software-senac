package br.senac.recursos.professor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.seguranca.IdentidadesDeTeste;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.TokenService;
import jakarta.servlet.ServletException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Apoio dos testes de API dos professores: corpos, chamadas por perfil e limpeza do banco. */
final class ProfessoresDeTeste {

    private ProfessoresDeTeste() {
    }

    /** Monta o JSON. Os valores de restrito e competencias entram como foram escritos (podem ser inválidos). */
    static String corpo(String nome, String restritoEmJson, String competenciasEmJson) {
        StringBuilder json = new StringBuilder("{");
        if (nome != null) {
            json.append("\"nome\":\"").append(nome).append("\",");
        }
        if (restritoEmJson != null) {
            json.append("\"restrito\":").append(restritoEmJson).append(",");
        }
        if (competenciasEmJson != null) {
            json.append("\"competencias\":").append(competenciasEmJson).append(",");
        }
        if (json.charAt(json.length() - 1) == ',') {
            json.deleteCharAt(json.length() - 1);
        }
        return json.append("}").toString();
    }

    static String corpoValido(String nome) {
        return corpo(nome, "false", "[\"Banco de Dados\",\"Java\"]");
    }

    static ResultActions cadastra(MockMvc mvc, TokenService tokens, Perfil perfil, String json) throws Exception {
        return executa(mvc, post("/api/professores")
                .header(HttpHeaders.AUTHORIZATION, IdentidadesDeTeste.bearerDe(tokens, perfil))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    static ResultActions altera(MockMvc mvc, TokenService tokens, Perfil perfil, long id, String json)
            throws Exception {
        return executa(mvc, put("/api/professores/" + id)
                .header(HttpHeaders.AUTHORIZATION, IdentidadesDeTeste.bearerDe(tokens, perfil))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    static ResultActions exclui(MockMvc mvc, TokenService tokens, Perfil perfil, long id) throws Exception {
        return executa(mvc, delete("/api/professores/" + id)
                .header(HttpHeaders.AUTHORIZATION, IdentidadesDeTeste.bearerDe(tokens, perfil)));
    }

    /** Cadastra pela API e devolve o id. Confere o 201 antes: se o cadastro falhar, o teste falha ali. */
    static long cadastraEDevolveId(MockMvc mvc, TokenService tokens, String json) throws Exception {
        String resposta = cadastra(mvc, tokens, Perfil.ADMINISTRADOR, json)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.parse(resposta).read("$.id", Long.class);
    }

    static void limpaBanco(JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM competencia");
        jdbc.update("DELETE FROM professor");
        jdbc.update("DELETE FROM sala");
        jdbc.update("DELETE FROM recurso");
    }

    /** Uma exceção que escapa da API é uma resposta errada (deveria ser 4xx): vira falha de asserção, e não erro. */
    private static ResultActions executa(MockMvc mvc, MockHttpServletRequestBuilder requisicao) throws Exception {
        try {
            return mvc.perform(requisicao);
        } catch (ServletException erro) {
            throw new AssertionError("A API deixou escapar uma exceção em vez de responder com erro 4xx: "
                    + erro.getMessage(), erro);
        }
    }
}
