package br.senac.recursos.sala;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TestcontainersConfiguration;
import br.senac.recursos.seguranca.IdentidadesDeTeste;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.TokenService;
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
 * Cadastro de sala pela API (spec T-16, CA-01; caso TC-01). Banco real (Testcontainers).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.seguranca.chave-assinatura=chave-de-teste-com-pelo-menos-32-caracteres-123",
        "app.admin-inicial.email=admin-de-teste@exemplo.com",
        "app.admin-inicial.senha=senha-inicial-de-teste"})
class CadastroDeSalaApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokens;

    @Test
    void tc01_administradorCadastraSalaValidaERecebe201ComIdERestritoFalso() throws Exception {
        String corpo = """
                {"nome":"sala-102","tipoSala":"Laboratório","capacidade":30,"localizacao":"Bloco B, 1º andar"}
                """;

        mockMvc.perform(post("/api/salas")
                        .header(HttpHeaders.AUTHORIZATION, IdentidadesDeTeste.bearerDe(tokens, Perfil.ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("sala-102"))
                .andExpect(jsonPath("$.capacidade").value(30))
                .andExpect(jsonPath("$.restrito").value(false));
    }
}
