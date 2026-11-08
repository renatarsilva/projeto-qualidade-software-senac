package br.senac.recursos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CodificadorDeSenhaTest {

    private final CodificadorDeSenha codificador = new CodificadorDeSenha();

    @Test
    void tc01_hashComecaComPrefixoBcryptEDiferenteDaSenha() {
        // Arrange
        String senha = "senha-de-teste-1";

        // Act
        String hash = codificador.codifica(senha);

        // Assert
        assertThat(hash).startsWith("$2");
        assertThat(hash).isNotEqualTo(senha);
    }

    @Test
    void tc02_mesmaSenhaGeraHashesDiferentes() {
        // Arrange
        String senha = "senha-de-teste-1";

        // Act
        String primeiro = codificador.codifica(senha);
        String segundo = codificador.codifica(senha);

        // Assert
        assertThat(primeiro).isNotEqualTo(segundo);
    }
}
