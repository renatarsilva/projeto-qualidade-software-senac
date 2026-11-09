package br.senac.recursos.material;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Limpeza e tamanho dos textos do material, sem subir a aplicação (spec T-18, CA-02; casos TC-27 e TC-28).
 */
class TextosDoMaterialTest {

    private static final String MOTIVO_100 = "deve ter no máximo 100 caracteres";
    private static final String MOTIVO_30 = "deve ter no máximo 30 caracteres";

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private MaterialRequest pedido(String nome, String tipo, String patrimonio) {
        return new MaterialRequest(nome, tipo, patrimonio, false);
    }

    private List<String> motivos(MaterialRequest pedido, String campo) {
        return validador.validateProperty(pedido.limpo(), campo).stream()
                .map(ConstraintViolation::getMessage).collect(Collectors.toList());
    }

    @Test
    void tc27_aLimpezaTiraEspacosDasPontasEReduzOsRepetidosDoMeio() {
        MaterialRequest limpo = pedido("  Projetor   Epson ", "  a ", "PAT   123").limpo();

        assertThat(limpo.nome()).isEqualTo("Projetor Epson");
        assertThat(limpo.tipoMaterial()).isEqualTo("a");
        assertThat(limpo.patrimonio()).isEqualTo("PAT 123");
    }

    @Test
    void tc28_cemCaracteresSaoAceitosCentoEUmSaoRecusadosNoNomeENoTipo() {
        assertThat(motivos(pedido("n".repeat(100), "Projetor", "PAT-1"), "nome")).isEmpty();
        assertThat(motivos(pedido("n".repeat(101), "Projetor", "PAT-1"), "nome")).contains(MOTIVO_100);
        assertThat(motivos(pedido("Projetor", "t".repeat(100), "PAT-1"), "tipoMaterial")).isEmpty();
        assertThat(motivos(pedido("Projetor", "t".repeat(101), "PAT-1"), "tipoMaterial")).contains(MOTIVO_100);
    }

    @Test
    void tc28_trintaCaracteresSaoAceitosETrintaEUmSaoRecusadosNoPatrimonio() {
        assertThat(motivos(pedido("Projetor", "Projetor", "p".repeat(30)), "patrimonio")).isEmpty();
        assertThat(motivos(pedido("Projetor", "Projetor", "p".repeat(31)), "patrimonio")).contains(MOTIVO_30);
    }
}
