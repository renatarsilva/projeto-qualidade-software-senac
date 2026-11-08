package br.senac.recursos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PoliticaDeSenhaTest {

    private final PoliticaDeSenha politica = new PoliticaDeSenha();

    @Test
    void tc03_senhaComOitoCaracteresEAceita() {
        // Arrange
        String senha = "12345678";

        // Act
        boolean resultado = politica.valida(senha);

        // Assert
        assertThat(resultado).isTrue();
    }

    @Test
    void tc04_senhaComSeteCaracteresERecusada() {
        // Arrange
        String senha = "1234567";

        // Act
        boolean resultado = politica.valida(senha);

        // Assert
        assertThat(resultado).isFalse();
    }

    @Test
    void tc05a_senhaComSetentaEDoisBytesEAceita() {
        // Arrange
        String senha = "a".repeat(72);

        // Act
        boolean resultado = politica.valida(senha);

        // Assert
        assertThat(resultado).isTrue();
    }

    @Test
    void tc05b_senhaComSetentaETresBytesERecusada() {
        // Arrange
        String senha = "a".repeat(73);

        // Act
        boolean resultado = politica.valida(senha);

        // Assert
        assertThat(resultado).isFalse();
    }
}


