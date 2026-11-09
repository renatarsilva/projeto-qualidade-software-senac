package br.senac.recursos.sala;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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

/** Apoio dos testes de API das salas: corpo padrão, chamadas por perfil e limpeza do banco. */
final class SalasDeTeste {

    private SalasDeTeste() {
    }

    static String corpo(String nome, String tipoSala, String capacidade, String localizacao, String restrito) {
        StringBuilder json = new StringBuilder("{");
        if (nome != null) {
            json.append("\"nome\":\"").append(nome).append("\",");
        }
        if (tipoSala != null) {
            json.append("\"tipoSala\":\"").append(tipoSala).append("\",");
        }
        if (capacidade != null) {
            json.append("\"capacidade\":").append(capacidade).append(",");
        }
        if (localizacao != null) {
            json.append("\"localizacao\":\"").append(localizacao).append("\",");
        }
        if (restrito != null) {
            json.append("\"restrito\":").append(restrito).append(",");
        }
        if (json.charAt(json.length() - 1) == ',') {
            json.deleteCharAt(json.length() - 1);
        }
        return json.append("}").toString();
    }

    static String corpoValido(String nome) {
        return corpo(nome, "Laboratório", "30", "Bloco B", "false");
    }

    static ResultActions cadastra(MockMvc mvc, TokenService tokens, Perfil perfil, String json) throws Exception {
        return executa(mvc, post("/api/salas")
                .header(HttpHeaders.AUTHORIZATION, IdentidadesDeTeste.bearerDe(tokens, perfil))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    static ResultActions altera(MockMvc mvc, TokenService tokens, Perfil perfil, long id, String json)
            throws Exception {
        return executa(mvc, put("/api/salas/" + id)
                .header(HttpHeaders.AUTHORIZATION, IdentidadesDeTeste.bearerDe(tokens, perfil))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    /** Cadastra pela API e devolve o id gravado (o teste que chama já espera que o cadastro funcione). */
    static long cadastraEDevolveId(MockMvc mvc, TokenService tokens, String json) throws Exception {
        String resposta = cadastra(mvc, tokens, Perfil.ADMINISTRADOR, json)
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.parse(resposta).read("$.id", Long.class);
    }

    static void limpaBanco(JdbcTemplate jdbc) {
        // Na ordem das dependências: outras classes de teste deixam professores e materiais no mesmo banco.
        jdbc.update("DELETE FROM material");
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
