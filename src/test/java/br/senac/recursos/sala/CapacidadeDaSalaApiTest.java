package br.senac.recursos.sala;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TestcontainersConfiguration;
import br.senac.recursos.seguranca.IdentidadesDeTeste;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.TokenService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Capacidade pela API (spec T-16, CA-02; casos TC-05, TC-20, TC-21, TC-30 e TC-34).
 * O leitor de JSON não converte tipos: 30.5 não vira 30 e "30" não vira número (P-04).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.seguranca.chave-assinatura=chave-de-teste-com-pelo-menos-32-caracteres-123",
        "app.admin-inicial.email=admin-de-teste@exemplo.com",
        "app.admin-inicial.senha=senha-inicial-de-teste"})
class CapacidadeDaSalaApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokens;

    /** Cadastra uma sala com o literal JSON da capacidade exatamente como foi escrito. */
    private ResultActions cadastraComCapacidade(String nome, String capacidadeEmJson) throws Exception {
        String corpo = "{\"nome\":\"" + nome + "\",\"tipoSala\":\"Laboratório\",\"capacidade\":"
                + capacidadeEmJson + ",\"localizacao\":\"Bloco B\"}";
        return mockMvc.perform(post("/api/salas")
                .header(HttpHeaders.AUTHORIZATION, IdentidadesDeTeste.bearerDe(tokens, Perfil.ADMINISTRADOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    // TC-05, TC-20, TC-34 e o lado de cima do TC-21: valores que não são um inteiro válido
    @ParameterizedTest(name = "capacidade {0} é recusada como valor inválido")
    @ValueSource(strings = {"\"abc\"", "30.5", "\"30\"", "2147483648"})
    void capacidadeQueNaoEUmInteiroValidoEhRecusadaComValorInvalido(String capacidadeEmJson) throws Exception {
        cadastraComCapacidade("sala-120", capacidadeEmJson)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("capacidade")))
                .andExpect(jsonPath("$.erros[?(@.campo=='capacidade')].motivo").value(hasItem("valor inválido")));
    }

    // TC-30: zero e negativa
    @ParameterizedTest(name = "capacidade {0} é recusada: deve ser maior que zero")
    @ValueSource(strings = {"0", "-1"})
    void capacidadeZeroOuNegativaEhRecusadaComOMotivo(String capacidadeEmJson) throws Exception {
        cadastraComCapacidade("sala-121", capacidadeEmJson)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='capacidade')].motivo").value(hasItem("deve ser maior que zero")));
    }

    // TC-21: o maior inteiro do banco ainda é aceito
    @org.junit.jupiter.api.Test
    void tc21_oMaiorInteiroDoBancoEAceito() throws Exception {
        cadastraComCapacidade("sala-122", "2147483647")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.capacidade").value(2147483647));
    }
}
