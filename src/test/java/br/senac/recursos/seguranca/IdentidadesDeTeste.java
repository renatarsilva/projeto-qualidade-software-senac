package br.senac.recursos.seguranca;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Gera o cabeçalho Authorization de um perfil, para os testes de API das outras funcionalidades.
 * O usuário é real: fica gravado no banco do teste, porque a API confere o usuário a cada chamada (CA-15).
 */
public final class IdentidadesDeTeste {

    // Hash de mentira, só para preencher a coluna: ninguém faz login com estes usuários de teste.
    private static final String HASH_DE_TESTE = "$2a$10$abcdefghijklmnopqrstuuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0";

    private static volatile JdbcTemplate jdbc;

    private IdentidadesDeTeste() {
    }

    /** Chamado pelo IdentidadesDeTesteListener com o banco do contexto do teste. */
    static void usaBanco(JdbcTemplate banco) {
        jdbc = banco;
    }

    /** Token de um usuário real com este perfil: cria o usuário se não existir e o deixa ativo e com o perfil pedido. */
    public static String bearerDe(TokenService tokens, Perfil perfil) {
        return bearerDoUsuario(tokens, garanteUsuario(perfil), perfil);
    }

    /** Token do usuário que já está no banco com este id. */
    public static String bearerDoUsuario(TokenService tokens, long id, Perfil perfil) {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(id);
        when(usuario.getPerfil()).thenReturn(perfil);
        return "Bearer " + tokens.emite(usuario).valor();
    }

    /** Cria (ou reativa) o usuário de teste do perfil e devolve o id. */
    public static long garanteUsuario(Perfil perfil) {
        String email = "teste-" + perfil.name().toLowerCase() + "@exemplo.com";
        return jdbc.queryForObject(
                "INSERT INTO usuario (nome, email, senha_hash, perfil, ativo) VALUES (?, ?, ?, ?, true) "
                        + "ON CONFLICT (email) DO UPDATE SET perfil = EXCLUDED.perfil, ativo = true RETURNING id",
                Long.class, "Usuário de teste", email, HASH_DE_TESTE, perfil.name());
    }
}
