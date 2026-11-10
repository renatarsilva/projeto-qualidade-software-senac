package br.senac.recursos.erro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TesteDeApiComBanco;
import br.senac.recursos.seguranca.IdentidadesDeTeste;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.TokenService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Formato único de erro da API (spec T-22, CA-01 a CA-04, CA-06 a CA-09, CA-11 a CA-13 e CA-15).
 * Toda resposta de erro é application/problem+json, com título em português e sem nenhum termo da lista única.
 */
@TesteDeApiComBanco
@Import(SegurancaDeTesteDeErros.class)
@ExtendWith(OutputCaptureExtension.class)
class ErrosDaApiTest {

    private static final String BASE = "/api/testes-de-erro";
    private static final String GENERICA = "Esta operação conflita com outro registro";
    private static final String EM_USO = "Este registro está em uso e não pode ser removido";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepararTabelasDeApoio() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS teste_unico (valor VARCHAR(50) UNIQUE)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS teste_periodo (periodo tstzrange, "
                + "EXCLUDE USING gist (periodo WITH &&))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS teste_pai (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS teste_filho (pai_id BIGINT NOT NULL REFERENCES teste_pai (id))");
        jdbc.update("DELETE FROM teste_filho");
        jdbc.update("DELETE FROM teste_pai");
        jdbc.update("DELETE FROM teste_unico");
        jdbc.update("DELETE FROM teste_periodo");
    }

    /** Executa pedindo JSON (como um cliente da API) e transforma exceção que escapa em falha de asserção. */
    private ResultActions executa(MockHttpServletRequestBuilder requisicao) throws Exception {
        try {
            return mvc.perform(requisicao.accept(MediaType.APPLICATION_JSON));
        } catch (ServletException erro) {
            throw new AssertionError("A API deixou escapar uma exceção em vez de devolver o formato de erro: "
                    + erro.getMessage(), erro);
        }
    }

    private String corpoDe(ResultActions resultado) throws Exception {
        MvcResult mvcResult = resultado.andReturn();
        return mvcResult.getResponse().getContentAsString();
    }

    private ResultActions erroEsperado(MockHttpServletRequestBuilder requisicao, int status, String titulo)
            throws Exception {
        ResultActions resultado = executa(requisicao)
                .andExpect(status().is(status))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value(titulo));
        TermosProibidos.naoTem(corpoDe(resultado));
        return resultado;
    }

    private MockHttpServletRequestBuilder json(String metodoEPath, String corpo) {
        return MockMvcRequestBuilders.request(HttpMethod.POST, metodoEPath)
                .contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    // CA-06: um erro de cada tipo, com Accept: application/json (TC-11, TC-15, TC-32 e TC-41)
    @ParameterizedTest(name = "{0} {1} devolve {2} \"{3}\"")
    @CsvSource(delimiter = '|', value = {
            "POST|/cadastros|400|Dados inválidos",
            "GET|/acesso-negado|403|Acesso negado",
            "GET|/nao-encontrado|404|Recurso não encontrado",
            "DELETE|/numero/1|405|Método não permitido",
            "GET|/conflito|409|Conflito",
            "POST|/conflito|409|Conflito",
            "POST|/so-json|415|Formato não suportado",
            "GET|/falha-inesperada|500|Erro inesperado"})
    void tc11e15e41_umErroDeCadaTipoTemOFormatoUnicoETituloEmPortugues(String metodo, String rota, int status,
            String titulo) throws Exception {
        MockHttpServletRequestBuilder requisicao = MockMvcRequestBuilders.request(HttpMethod.valueOf(metodo),
                BASE + rota);
        if (rota.equals("/cadastros")) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content("{\"capacidade\":0}");
        } else if (rota.equals("/so-json")) {
            requisicao.contentType(MediaType.TEXT_PLAIN).content("texto");
        }
        erroEsperado(requisicao, status, titulo)
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value(BASE + rota));
    }

    @Test
    void tc01a04_capacidadeInvalidaApontaOCampoEOMotivo() throws Exception {
        erroEsperado(json(BASE + "/cadastros", "{\"capacidade\":0}"), 400, "Dados inválidos")
                .andExpect(jsonPath("$.erros[?(@.campo=='capacidade')].motivo").value(hasItem("deve ser maior que zero")));
        erroEsperado(json(BASE + "/cadastros", "{\"capacidade\":-1}"), 400, "Dados inválidos")
                .andExpect(jsonPath("$.erros[?(@.campo=='capacidade')].motivo").value(hasItem("deve ser maior que zero")));
        erroEsperado(json(BASE + "/cadastros", "{}"), 400, "Dados inválidos")
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("capacidade")));
        executa(json(BASE + "/cadastros", "{\"capacidade\":1}")).andExpect(status().isOk()); // TC-02: limite válido
    }

    @Test
    void tc05_idInexistenteDa404ComAMensagemPadrao() throws Exception {
        erroEsperado(get(BASE + "/nao-encontrado"), 404, "Recurso não encontrado")
                .andExpect(jsonPath("$.detail").value("Recurso não encontrado"));
    }

    @Test
    void tc06_conflitoDeRegraTrazAMensagemEspecifica() throws Exception {
        erroEsperado(get(BASE + "/conflito"), 409, "Conflito")
                .andExpect(jsonPath("$.detail").value("Já existe um registro com esse valor"));
    }

    @Test
    void tc07e08e09_erroInesperadoDa500NeutroEODetalheTecnicoVaiSoParaOLog(CapturedOutput saida) throws Exception {
        erroEsperado(get(BASE + "/falha-inesperada"), 500, "Erro inesperado")
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente mais tarde."));
        erroEsperado(get(BASE + "/falha-de-dados"), 500, "Erro inesperado")
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente mais tarde."));

        assertThat(saida.getAll()).contains("SELECT * FROM salas");
    }

    @Test
    void tc16_jsonMalformadoDa400() throws Exception {
        erroEsperado(json(BASE + "/cadastros", "{capacidade: "), 400, "Dados inválidos");
    }

    @Test
    void tc17_idQueNaoEUmNumeroDa400ApontandoOId() throws Exception {
        erroEsperado(get(BASE + "/numero/abc"), 400, "Dados inválidos")
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("id")));
    }

    @Test
    void tc18_rotaQueNaoExisteNaApiDa404() throws Exception {
        // Dentro das rotas liberadas de teste: fora delas, a segurança nega por padrão antes de procurar a rota (401 ou 403).
        erroEsperado(get(BASE + "/rota-que-nao-existe-de-jeito-nenhum"), 404, "Recurso não encontrado");
    }

    @ParameterizedTest(name = "{0} vira 409 com a mensagem fixa")
    @CsvSource(delimiter = '|', value = {"/simula-unique|" + GENERICA, "/simula-exclude|" + GENERICA,
            "/simula-fk|" + EM_USO})
    void tc21e22e27_violacoesSimuladasDoBancoDao409ComMensagemFixa(String rota, String mensagem) throws Exception {
        erroEsperado(get(BASE + rota), 409, "Conflito").andExpect(jsonPath("$.detail").value(mensagem));
    }

    @Test
    void tc23_valorDeEnumQueNaoExisteDa400ApontandoOEstado() throws Exception {
        erroEsperado(json(BASE + "/estados", "{\"estado\":\"ABC\"}"), 400, "Dados inválidos")
                .andExpect(jsonPath("$.erros[?(@.campo=='estado')].motivo").value(hasItem("valor inválido")));
    }

    @Test
    void tc24_regraEntreDoisCamposApontaOCampoQueARegraIndica() throws Exception {
        erroEsperado(json(BASE + "/periodos",
                "{\"inicio\":\"2026-11-09T10:00:00Z\",\"termino\":\"2026-11-09T09:00:00Z\"}"), 400, "Dados inválidos")
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("termino")));
    }

    @Test
    void tc28_chaveEstrangeiraRealDa409ComAMensagemFixaEORegistroContinua() throws Exception {
        jdbc.update("INSERT INTO teste_pai (id) VALUES (1)");
        jdbc.update("INSERT INTO teste_filho (pai_id) VALUES (1)");

        erroEsperado(delete(BASE + "/pais/1"), 409, "Conflito").andExpect(jsonPath("$.detail").value(EM_USO));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM teste_pai", Long.class)).isEqualTo(1);
    }

    @Test
    void tc29_uniqueRealPassandoPeloServicoDa409ComAMensagemFixaESoUmRegistro() throws Exception {
        executa(json(BASE + "/unico", "{\"valor\":\"x\",\"verificar\":false}")).andExpect(status().isOk());

        erroEsperado(json(BASE + "/unico", "{\"valor\":\"x\",\"verificar\":false}"), 409, "Conflito")
                .andExpect(jsonPath("$.detail").value(GENERICA));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM teste_unico", Long.class)).isEqualTo(1);
    }

    @Test
    void tc30_excludeRealDa409ComAMensagemFixaESoUmRegistro() throws Exception {
        executa(json(BASE + "/periodos-reais",
                "{\"inicio\":\"2026-11-09T10:00:00Z\",\"termino\":\"2026-11-09T12:00:00Z\"}")).andExpect(status().isOk());

        erroEsperado(json(BASE + "/periodos-reais",
                "{\"inicio\":\"2026-11-09T11:00:00Z\",\"termino\":\"2026-11-09T13:00:00Z\"}"), 409, "Conflito")
                .andExpect(jsonPath("$.detail").value(GENERICA));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM teste_periodo", Long.class)).isEqualTo(1);
    }

    @Test
    void tc31_quandoOServicoPegaORepetidoAMensagemEEspecificaENaoAGenerica() throws Exception {
        executa(json(BASE + "/unico", "{\"valor\":\"y\",\"verificar\":true}")).andExpect(status().isOk());

        erroEsperado(json(BASE + "/unico", "{\"valor\":\"y\",\"verificar\":true}"), 409, "Conflito")
                .andExpect(jsonPath("$.detail").value("Já existe um registro com esse valor"));
    }

    @Test
    void tc33a36_leituraDoJsonSemConversaoDeTipos() throws Exception {
        for (String capacidade : new String[] {"\"abc\"", "30.5", "\"30\"", "2147483648"}) {
            erroEsperado(json(BASE + "/cadastros", "{\"capacidade\":" + capacidade + "}"), 400, "Dados inválidos")
                    .andExpect(jsonPath("$.erros[?(@.campo=='capacidade')].motivo").value(hasItem("valor inválido")));
        }
        executa(json(BASE + "/cadastros", "{\"capacidade\":2147483647}")).andExpect(status().isOk());
    }

    @Test
    void tc37_acessoNegadoLancadoPeloServicoDa403NeutroENaoDa500() throws Exception {
        erroEsperado(get(BASE + "/acesso-negado"), 403, "Acesso negado")
                .andExpect(jsonPath("$.detail").value("Você não tem permissão para esta operação"));
    }

    // CA-15: a ordem dos erros 401, 403 por perfil, 404, 403 por dono, 400 e 409 (TC-43 a TC-47)
    @Test
    void tc43_semAutenticacaoEComCorpoInvalidoDa401() throws Exception {
        mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tc44_perfilSemPermissaoEIdInexistenteDa403PorPerfil() throws Exception {
        mvc.perform(MockMvcRequestBuilders.put("/api/salas/999999")
                        .header(HttpHeaders.AUTHORIZATION, IdentidadesDeTeste.bearerDe(tokens, Perfil.SOLICITANTE))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tc45_idInexistenteEComCorpoInvalidoDa404() throws Exception {
        mvc.perform(MockMvcRequestBuilders.put("/api/salas/999999")
                        .header(HttpHeaders.AUTHORIZATION, IdentidadesDeTeste.bearerDe(tokens, Perfil.ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void tc46_quemNaoEODonoEComCorpoInvalidoDa403PorDono() throws Exception {
        erroEsperado(json(BASE + "/dono", "{\"capacidade\":0}"), 403, "Acesso negado");
    }

    @Test
    void tc47_corpoInvalidoEConflitoAoMesmoTempoDa400() throws Exception {
        erroEsperado(json(BASE + "/conflito-e-corpo-invalido", "{\"capacidade\":0}"), 400, "Dados inválidos");
    }
}
