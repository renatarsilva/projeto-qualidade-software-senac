package br.senac.recursos.sala;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Regra da capacidade, sem subir a aplicação (spec T-16, CA-02; casos TC-02 a TC-04).
 */
class CapacidadeDaSalaTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<ConstraintViolation<SalaRequest>> valida(int capacidade) {
        SalaRequest pedido = new SalaRequest("sala-102", "Laboratório", capacidade, "Bloco B", null);
        return validador.validateProperty(pedido, "capacidade");
    }

    @Test
    void tc02_capacidadeUmEAceita() {
        // Act
        Set<ConstraintViolation<SalaRequest>> erros = valida(1);

        // Assert
        assertThat(erros).isEmpty();
    }

    @Test
    void tc03_capacidadeZeroERecusadaComOMotivo() {
        // Act
        Set<ConstraintViolation<SalaRequest>> erros = valida(0);

        // Assert
        assertThat(erros).hasSize(1);
        assertThat(erros.iterator().next().getMessage()).isEqualTo("deve ser maior que zero");
    }

    @Test
    void tc04_capacidadeNegativaERecusadaComOMesmoMotivo() {
        // Act
        Set<ConstraintViolation<SalaRequest>> erros = valida(-1);

        // Assert
        assertThat(erros).hasSize(1);
        assertThat(erros.iterator().next().getMessage()).isEqualTo("deve ser maior que zero");
    }
}
