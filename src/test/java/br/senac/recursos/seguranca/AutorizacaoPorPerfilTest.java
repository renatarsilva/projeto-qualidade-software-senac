package br.senac.recursos.seguranca;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Perfil por rota (spec T-15, CA-07 e CA-08; casos TC-17 a TC-21). Tabela de decisão: identidade x rota.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RegrasDeAcessoDeTeste.class})
@TestPropertySource(properties = {
        "app.seguranca.chave-assinatura=chave-de-teste-com-pelo-menos-32-caracteres-123",
        "app.admin-inicial.email=admin-de-teste@exemplo.com",
        "app.admin-inicial.senha=senha-inicial-de-teste"})
class AutorizacaoPorPerfilTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokens;

    private MockHttpServletRequestBuilder chamada(String rota, String identidade) {
        MockHttpServletRequestBuilder requisicao = get(rota);
        if (!"ANONIMO".equals(identidade)) {
            requisicao.header(HttpHeaders.AUTHORIZATION,
                    IdentidadesDeTeste.bearerDe(tokens, Perfil.valueOf(identidade)));
        }
        return requisicao;
    }

    // Colunas: identidade, rota, status esperado.
    @ParameterizedTest(name = "{0} em {1} recebe {2}")
    @CsvSource({
            // TC-17 a TC-20: rota só do Administrador
            "ANONIMO,       /api/teste/administrador, 401",
            "SOLICITANTE,   /api/teste/administrador, 403",
            "RESPONSAVEL,   /api/teste/administrador, 403",
            "ADMINISTRADOR, /api/teste/administrador, 200",
            // TC-21: rota de qualquer autenticado
            "ANONIMO,       /api/teste/autenticado,   401",
            "SOLICITANTE,   /api/teste/autenticado,   200",
            "RESPONSAVEL,   /api/teste/autenticado,   200",
            "ADMINISTRADOR, /api/teste/autenticado,   200",
            // CA-08: rota sem regra declarada é negada para todos os perfis
            "SOLICITANTE,   /api/teste/sem-regra,     403",
            "RESPONSAVEL,   /api/teste/sem-regra,     403",
            "ADMINISTRADOR, /api/teste/sem-regra,     403"
    })
    void tc17a21_tabelaDeDecisaoDePerfilPorRota(String identidade, String rota, int statusEsperado) throws Exception {
        mockMvc.perform(chamada(rota, identidade)).andExpect(status().is(statusEsperado));
    }

    @Test
    void tc18_respostaDeAcessoNegadoNaoRevelaDetalheInterno() throws Exception {
        mockMvc.perform(chamada("/api/teste/administrador", "SOLICITANTE"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Acesso negado"))
                .andExpect(jsonPath("$.instance").value("/api/teste/administrador"));
    }
}
