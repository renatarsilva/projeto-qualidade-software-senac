package br.senac.recursos.sala;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Tamanho e limpeza do tipo e da localização, sem subir a aplicação
 * (spec T-16, CA-03 e CA-11; casos TC-22 e TC-36).
 */
class TextosDaSalaTest {

    private static final String MOTIVO_TAMANHO = "deve ter no máximo 100 caracteres";

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private List<String> motivosDoTipo(SalaRequest pedido) {
        return validador.validateProperty(pedido, "tipoSala").stream()
                .map(ConstraintViolation::getMessage).collect(Collectors.toList());
    }

    private List<String> motivosDaLocalizacao(SalaRequest pedido) {
        return validador.validateProperty(pedido, "localizacao").stream()
                .map(ConstraintViolation::getMessage).collect(Collectors.toList());
    }

    private SalaRequest comTipo(String tipo) {
        return new SalaRequest("sala-102", tipo, 30, "Bloco B", null);
    }

    private SalaRequest comLocalizacao(String localizacao) {
        return new SalaRequest("sala-102", "Laboratório", 30, localizacao, null);
    }

    @Test
    void tc22_cemCaracteresSaoAceitosECentoEUmSaoRecusadosNoTipoENaLocalizacao() {
        assertThat(motivosDoTipo(comTipo("t".repeat(100)))).isEmpty();
        assertThat(motivosDoTipo(comTipo("t".repeat(101)))).contains(MOTIVO_TAMANHO);
        assertThat(motivosDaLocalizacao(comLocalizacao("l".repeat(100)))).isEmpty();
        assertThat(motivosDaLocalizacao(comLocalizacao("l".repeat(101)))).contains(MOTIVO_TAMANHO);
    }

    @Test
    void tc36_otamanhoContaDepoisDaLimpezaDeEspacos() {
        // 100 letras com espaços nas pontas: depois da limpeza são 100, então é aceito.
        SalaRequest comEspacosNasPontas = comTipo("  " + "t".repeat(100) + "  ").limpo();
        assertThat(motivosDoTipo(comEspacosNasPontas)).isEmpty();

        // 101 letras, mesmo depois da limpeza, continuam passando de 100.
        SalaRequest grandeDemais = comTipo("  " + "t".repeat(101) + "  ").limpo();
        assertThat(motivosDoTipo(grandeDemais)).contains(MOTIVO_TAMANHO);
    }

    @Test
    void p05_aLimpezaTiraEspacosDasPontasEReduzOsRepetidosDoMeio() {
        SalaRequest limpo = new SalaRequest("sala-102", "  Laboratório   de   Redes ", 30, "  Bloco   B ", null).limpo();

        assertThat(limpo.tipoSala()).isEqualTo("Laboratório de Redes");
        assertThat(limpo.localizacao()).isEqualTo("Bloco B");
    }
}
