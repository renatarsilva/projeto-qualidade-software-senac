package br.senac.recursos.seguranca;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Gera o cabeçalho Authorization de um perfil, para os testes de API das outras funcionalidades. */
public final class IdentidadesDeTeste {

    private IdentidadesDeTeste() {
    }

    public static String bearerDe(TokenService tokens, Perfil perfil) {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(1L);
        when(usuario.getPerfil()).thenReturn(perfil);
        return "Bearer " + tokens.emite(usuario).valor();
    }
}
