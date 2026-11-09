package br.senac.recursos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Primeiro administrador (spec T-15, CA-16; casos TC-40 a TC-42).
 * O repositório é simulado: aqui só se testa a decisão de criar, ou não, o administrador inicial.
 */
class AdministradorInicialTest {

    private UsuarioRepository usuarios;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
    }

    private AdministradorInicial comAdministrador(String email, String senha) {
        return new AdministradorInicial(usuarios, new CodificadorDeSenha(), email, senha);
    }

    @Test
    void tc40_semAdministradorECom_variaveisCriaAdministradorAtivoComSenhaEmHash() {
        // Arrange
        when(usuarios.existsByPerfil(Perfil.ADMINISTRADOR)).thenReturn(false);
        AdministradorInicial inicial = comAdministrador("admin@exemplo.com", "senha-inicial-1");

        // Act
        inicial.garantirAdministrador();

        // Assert
        ArgumentCaptor<Usuario> gravado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(gravado.capture());
        Usuario admin = gravado.getValue();
        assertThat(admin.getEmail()).isEqualTo("admin@exemplo.com");
        assertThat(admin.getPerfil()).isEqualTo(Perfil.ADMINISTRADOR);
        assertThat(admin.isAtivo()).isTrue();
        assertThat(admin.getSenhaHash()).startsWith("$2").isNotEqualTo("senha-inicial-1");
    }

    @Test
    void tc41_semAdministradorESemVariaveisRecusaASubidaComMensagemClara() {
        // Arrange
        when(usuarios.existsByPerfil(Perfil.ADMINISTRADOR)).thenReturn(false);
        AdministradorInicial inicial = comAdministrador("", "");

        // Act + Assert
        assertThatThrownBy(inicial::garantirAdministrador)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_ADMIN_EMAIL")
                .hasMessageContaining("APP_ADMIN_SENHA");
        verify(usuarios, never()).save(any());
    }

    @Test
    void tc42_comAdministradorJaExistenteNaoCriaOutroMesmoComVariaveis() {
        // Arrange
        when(usuarios.existsByPerfil(Perfil.ADMINISTRADOR)).thenReturn(true);
        AdministradorInicial inicial = comAdministrador("admin@exemplo.com", "senha-inicial-1");

        // Act
        inicial.garantirAdministrador();

        // Assert
        verify(usuarios, never()).save(any());
    }
}
