package br.senac.recursos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.TestcontainersConfiguration;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Gestão de usuários pelo Administrador (spec T-15, CA-09, CA-10 e CA-14;
 * casos TC-23 a TC-28, TC-29 e TC-43). Banco real (Testcontainers).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.seguranca.chave-assinatura=chave-de-teste-com-pelo-menos-32-caracteres-123",
        "app.admin-inicial.email=admin-de-teste@exemplo.com",
        "app.admin-inicial.senha=senha-inicial-de-teste"})
@ExtendWith(OutputCaptureExtension.class)
class GestaoDeUsuariosApiTest {

    private static final String SENHA = "senha-de-teste-1";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    private final CodificadorDeSenha codificador = new CodificadorDeSenha();

    @BeforeEach
    void limpar() {
        jdbc.update("DELETE FROM usuario");
    }

    private String bearerDoAdministrador() {
        return IdentidadesDeTeste.bearerDe(tokens, Perfil.ADMINISTRADOR);
    }

    private ResultActions executa(MockHttpServletRequestBuilder requisicao) throws Exception {
        try {
            return mvc.perform(requisicao);
        } catch (ServletException erro) {
            throw new AssertionError("A API deixou escapar uma exceção em vez de responder 4xx: " + erro.getMessage(),
                    erro);
        }
    }

    private String corpoDoUsuario(String nome, String email, String senha, String perfil) {
        return "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"" + senha
                + "\",\"perfil\":\"" + perfil + "\"}";
    }

    private ResultActions criaUsuario(String corpo) throws Exception {
        return executa(post("/api/usuarios")
                .header(HttpHeaders.AUTHORIZATION, bearerDoAdministrador())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private long criaEDevolveId(String email, String perfil) throws Exception {
        String resposta = criaUsuario(corpoDoUsuario("Usuário de teste", email, SENHA, perfil))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.parse(resposta).read("$.id", Long.class);
    }

    private ResultActions patchDe(long id, String caminho, String corpo) throws Exception {
        return patchComo(bearerDoAdministrador(), id, caminho, corpo);
    }

    private ResultActions patchComo(String bearer, long id, String caminho, String corpo) throws Exception {
        return executa(patch("/api/usuarios/" + id + "/" + caminho)
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private ResultActions login(String email, String senha) throws Exception {
        return executa(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"));
    }

    private long insereAdministrador(String email, boolean ativo) {
        return jdbc.queryForObject(
                "INSERT INTO usuario (nome, email, senha_hash, perfil, ativo) "
                        + "VALUES ('Administrador de teste', ?, ?, 'ADMINISTRADOR', ?) RETURNING id",
                Long.class, email, codificador.codifica(SENHA), ativo);
    }

    @Test
    void tc23_administradorCriaSolicitanteERespostaNaoTraSenhaNemHash() throws Exception {
        criaUsuario(corpoDoUsuario("Marina Souza", "marina@exemplo.com", SENHA, "SOLICITANTE"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("marina@exemplo.com"))
                .andExpect(jsonPath("$.perfil").value("SOLICITANTE"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void tc24_perfilQueNaoExisteDa400ApontandoOPerfil() throws Exception {
        criaUsuario(corpoDoUsuario("Marina Souza", "marina@exemplo.com", SENHA, "GERENTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("perfil")));
    }

    @Test
    void tc25_emailJaCadastradoDa409() throws Exception {
        criaUsuario(corpoDoUsuario("Marina Souza", "marina@exemplo.com", SENHA, "SOLICITANTE"))
                .andExpect(status().isCreated());

        criaUsuario(corpoDoUsuario("Outra Marina", "marina@exemplo.com", SENHA, "SOLICITANTE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um usuário com esse e-mail"));
    }

    @Test
    void tc26_trocarOPerfilFazOProximoLoginTrazerOPerfilNovo() throws Exception {
        long id = criaEDevolveId("marina@exemplo.com", "SOLICITANTE");
        insereAdministrador("outro-admin@exemplo.com", true); // o sistema nunca fica sem administrador

        patchDe(id, "perfil", "{\"perfil\":\"ADMINISTRADOR\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ADMINISTRADOR"));

        String resposta = login("marina@exemplo.com", SENHA).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.parse(resposta).read("$.token", String.class);
        executa(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void tc27_desativarUsuarioImpedeOLoginEMantemOUsuarioNoBanco() throws Exception {
        long id = criaEDevolveId("marina@exemplo.com", "SOLICITANTE");

        patchDe(id, "ativo", "{\"ativo\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        login("marina@exemplo.com", SENHA).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE id = ?", Long.class, id)).isEqualTo(1);
    }

    @Test
    void tc28_desativarOuRebaixarOUnicoAdministradorAtivoDa409() throws Exception {
        long unico = insereAdministrador("unico-admin@exemplo.com", true);
        String comoUnico = IdentidadesDeTeste.bearerDoUsuario(tokens, unico, Perfil.ADMINISTRADOR);

        patchComo(comoUnico, unico, "ativo", "{\"ativo\":false}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("O sistema precisa de pelo menos um administrador"));
        patchComo(comoUnico, unico, "perfil", "{\"perfil\":\"SOLICITANTE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("O sistema precisa de pelo menos um administrador"));

        // Com um segundo administrador ativo, desativar o primeiro é permitido.
        insereAdministrador("segundo-admin@exemplo.com", true);
        patchComo(comoUnico, unico, "ativo", "{\"ativo\":false}").andExpect(status().isOk());
    }

    @Test
    void tc29_nenhumaSenhaApareceNoLog(CapturedOutput saida) throws Exception {
        String senhaSecreta = "senha-secreta-que-nao-pode-vazar-9";
        criaUsuario(corpoDoUsuario("Marina Souza", "marina@exemplo.com", senhaSecreta, "SOLICITANTE"))
                .andExpect(status().isCreated());
        login("marina@exemplo.com", "senha-errada-qualquer-1").andExpect(status().isUnauthorized());

        assertThat(saida.getAll()).doesNotContain(senhaSecreta).doesNotContain("senha-errada-qualquer-1");
    }

    @Test
    void tc04e43_senhaCurtaOuMaiorQueSetentaEDoisBytesDa400ComMensagemSimples() throws Exception {
        String mensagem = "A senha deve ter de 8 a 72 caracteres (acentos e símbolos contam mais)";

        criaUsuario(corpoDoUsuario("Marina Souza", "marina@exemplo.com", "1234567", "SOLICITANTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='senha')].motivo").value(hasItem(mensagem)));

        // 40 letras acentuadas: 40 caracteres, mas 80 bytes. Passa de 72 e não pode ser truncada em silêncio.
        criaUsuario(corpoDoUsuario("Marina Souza", "marina@exemplo.com", "á".repeat(40), "SOLICITANTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='senha')].motivo").value(hasItem(mensagem)));
    }
}
