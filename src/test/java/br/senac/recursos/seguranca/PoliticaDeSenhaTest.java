package br.senac.recursos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PoliticaDeSenhaTest {

    private final PoliticaDeSenha politica = new PoliticaDeSenha();

    @Test
    void tc03_senhaComOitoCaracteresEAceita() {
        // Arrange
        String senha = "12345678"; // PREENCHA: uma senha com exatamente 8 caracteres

        // Act
        boolean resultado = politica.valida(senha);

        // Assert
        assertThat(resultado).isTrue(); // PREENCHA: isTrue() ou isFalse()?
    }

    @Test
    void tc04_senhaComSeteCaracteresERecusada() {
        // Arrange
        String senha = "1234567"; // PREENCHA: uma senha com exatamente 7 caracteres

        // Act
        boolean resultado = politica.valida(senha);

        // Assert
        assertThat(resultado).isFalse(); // PREENCHA: isTrue() ou isFalse()?
    }
}
