package br.senac.recursos.sala;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Padrão do nome da sala, sem subir a aplicação (spec T-16, CA-03 e CA-10; casos TC-06, TC-07, TC-09, TC-27, TC-28).
 * O nome é "sala-" seguido de 2 ou 3 dígitos, em minúsculas, sem espaços (RN-16, P-01).
 */
class NomeDaSalaTest {

    private static final String MOTIVO_PADRAO = "deve seguir o padrão sala-NN";

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private List<String> motivos(String nome) {
        SalaRequest pedido = new SalaRequest(nome, "Laboratório", 30, "Bloco B", null);
        Set<ConstraintViolation<SalaRequest>> erros = validador.validateProperty(pedido, "nome");
        return erros.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList());
    }

    @Test
    void tc06_nomeAusenteERecusado() {
        assertThat(motivos(null)).contains("deve ser informado");
    }

    @Test
    void tc07_nomeSoComEspacosERecusado() {
        assertThat(motivos("   ")).contains("deve ser informado");
    }

    @Test
    void tc09_doisDigitosSaoAceitosEUmDigitoERecusado() {
        assertThat(motivos("sala-01")).isEmpty();
        assertThat(motivos("sala-1")).contains(MOTIVO_PADRAO);
    }

    @ParameterizedTest(name = "o nome \"{0}\" é recusado")
    @ValueSource(strings = {" sala-01", "sala-01 ", "Sala-01", "SALA-01", "sala 01", "saal-01", "sala-01x", "sala_01"})
    void tc27_nomesForaDoPadraoSaoRecusados(String nome) {
        assertThat(motivos(nome)).contains(MOTIVO_PADRAO);
    }

    @Test
    void tc28_tresDigitosSaoAceitosEQuatroSaoRecusados() {
        assertThat(motivos("sala-999")).isEmpty();
        assertThat(motivos("sala-1000")).contains(MOTIVO_PADRAO);
    }
}
