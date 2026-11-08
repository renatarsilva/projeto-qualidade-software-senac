package br.senac.recursos.seguranca;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Chamadas protegidas da API sem token ou com token (spec T-15, CA-05; casos TC-15 e TC-16).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RegrasDeAcessoDeTeste.class})
@TestPropertySource(properties = {
        "app.seguranca.chave-assinatura=chave-de-teste-com-pelo-menos-32-caracteres-123",
        "app.admin-inicial.email=admin-de-teste@exemplo.com",
        "app.admin-inicial.senha=senha-inicial-de-teste"})
class AutenticacaoDaApiTest {

    private static final String ROTA_PROTEGIDA = "/api/teste/autenticado";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokens;

    @Test
    void tc15_chamadaProtegidaSemCabecalhoAuthorizationDa401() throws Exception {
        mockMvc.perform(get(ROTA_PROTEGIDA))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Autenticação necessária"))
                .andExpect(jsonPath("$.instance").value(ROTA_PROTEGIDA));
    }

    @Test
    void tc16_cabecalhoSemBearerDa401() throws Exception {
        mockMvc.perform(get(ROTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, "Token qualquer-coisa"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Autenticação necessária"));
    }

    // Caso de controle (sem número na spec): prova que a rota abre quando o token é válido.
    @Test
    void controle_tokenValidoAcessaARotaProtegida() throws Exception {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(1L);
        when(usuario.getPerfil()).thenReturn(Perfil.SOLICITANTE);
        String token = tokens.emite(usuario).valor();

        mockMvc.perform(get(ROTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }
}
