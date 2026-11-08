package br.senac.recursos.seguranca;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Desativação e troca de perfil valem na hora (spec T-15, CA-15; casos TC-37 e TC-38).
 * O token continua válido, mas a API confere o usuário e o perfil atuais no banco a cada chamada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RegrasDeAcessoDeTeste.class})
@TestPropertySource(properties = {
        "app.seguranca.chave-assinatura=chave-de-teste-com-pelo-menos-32-caracteres-123",
        "app.admin-inicial.email=admin-de-teste@exemplo.com",
        "app.admin-inicial.senha=senha-inicial-de-teste"})
class ContaDoUsuarioNaHoraApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    private ResultActions chama(String rota, String bearer) throws Exception {
        return mvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    @Test
    void tc37_usuarioDesativadoComTokenValidoRecebe401() throws Exception {
        long id = IdentidadesDeTeste.garanteUsuario(Perfil.SOLICITANTE);
        String token = IdentidadesDeTeste.bearerDoUsuario(tokens, id, Perfil.SOLICITANTE);
        chama("/api/teste/autenticado", token).andExpect(status().isOk());

        jdbc.update("UPDATE usuario SET ativo = false WHERE id = ?", id);

        chama("/api/teste/autenticado", token)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Autenticação necessária"));
    }

    @Test
    void tc38_perfilNovoNoBancoVaiAntesDoPerfilGravadoNoToken() throws Exception {
        long id = IdentidadesDeTeste.garanteUsuario(Perfil.SOLICITANTE);
        String tokenDeSolicitante = IdentidadesDeTeste.bearerDoUsuario(tokens, id, Perfil.SOLICITANTE);
        chama("/api/teste/administrador", tokenDeSolicitante).andExpect(status().isForbidden());

        jdbc.update("UPDATE usuario SET perfil = 'ADMINISTRADOR' WHERE id = ?", id);

        // O token ainda diz SOLICITANTE, mas vale o perfil atual: ADMINISTRADOR.
        chama("/api/teste/administrador", tokenDeSolicitante).andExpect(status().isOk());
    }

    @Test
    void rebaixarUmAdministradorTiraOAcessoNaHoraMesmoComTokenDeAdministrador() throws Exception {
        long id = IdentidadesDeTeste.garanteUsuario(Perfil.ADMINISTRADOR);
        String tokenDeAdministrador = IdentidadesDeTeste.bearerDoUsuario(tokens, id, Perfil.ADMINISTRADOR);
        chama("/api/teste/administrador", tokenDeAdministrador).andExpect(status().isOk());

        jdbc.update("UPDATE usuario SET perfil = 'SOLICITANTE' WHERE id = ?", id);

        chama("/api/teste/administrador", tokenDeAdministrador).andExpect(status().isForbidden());
    }

    @Test
    void tokenDeUsuarioQueNaoExisteMaisRecebe401() throws Exception {
        String tokenDeFantasma = IdentidadesDeTeste.bearerDoUsuario(tokens, 987654321L, Perfil.ADMINISTRADOR);

        chama("/api/teste/autenticado", tokenDeFantasma).andExpect(status().isUnauthorized());
    }
}
