package br.senac.recursos.professor;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Padrão do nome do professor, sem subir a aplicação (spec T-17, CA-02; casos TC-03, TC-04, TC-19, TC-20, TC-26, TC-27).
 * O nome é primeironome-ultimonome-matricula: minúsculas sem acento e matrícula de exatamente 5 dígitos (RN-17).
 */
class NomeDoProfessorTest {

    private static final String MOTIVO_PADRAO = "deve seguir o padrão primeironome-ultimonome-matricula";

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private List<String> motivos(String nome) {
        ProfessorRequest pedido = new ProfessorRequest(nome, false, List.of());
        return validador.validateProperty(pedido, "nome").stream()
                .map(ConstraintViolation::getMessage).collect(Collectors.toList());
    }

    @Test
    void tc03_nomeAusenteOuSoComEspacosERecusado() {
        assertThat(motivos(null)).contains("deve ser informado");
        assertThat(motivos("   ")).contains("deve ser informado");
    }

    @Test
    void tc04_matriculaComCincoDigitosEAceitaEComQuatroERecusada() {
        assertThat(motivos("renata-silva-12345")).isEmpty();
        assertThat(motivos("renata-silva-1234")).contains(MOTIVO_PADRAO);
    }

    @Test
    void tc19_matriculaComCincoDigitosEAceitaEComSeisERecusada() {
        assertThat(motivos("renata-silva-12345")).isEmpty();
        assertThat(motivos("renata-silva-123456")).contains(MOTIVO_PADRAO);
    }

    @ParameterizedTest(name = "o nome \"{0}\" é recusado")
    @ValueSource(strings = {" renata-silva-12345", "renata-silva-12345 ", "Renata-Silva-12345", "renata silva 12345",
            "renata-silva", "renata-silva-abc", "renata-maria-silva-12345", "renata-silvá-12345"})
    void tc20_nomesForaDoPadraoSaoRecusados(String nome) {
        assertThat(motivos(nome)).contains(MOTIVO_PADRAO);
    }

    @ParameterizedTest(name = "o nome \"{0}\" é recusado")
    @ValueSource(strings = {"renata_silva-12345", "renata--silva-12345", "renata-silva--12345",
            "renata.silva-12345", "-renata-silva-12345", "renata-silva-12345-"})
    void tc27_separadoresInvalidosSaoRecusados(String nome) {
        assertThat(motivos(nome)).contains(MOTIVO_PADRAO);
    }

    @Test
    void tc26_nomeComCemCaracteresEAceitoEComCentoEUmERecusado() {
        // 46 letras + hífen + 47 letras + hífen + 5 dígitos = 100 caracteres.
        String cem = "a".repeat(46) + "-" + "b".repeat(47) + "-12345";
        String centoEUm = "a".repeat(46) + "-" + "b".repeat(48) + "-12345";

        assertThat(cem).hasSize(100);
        assertThat(motivos(cem)).isEmpty();
        assertThat(centoEUm).hasSize(101);
        assertThat(motivos(centoEUm)).isNotEmpty();
    }
}
