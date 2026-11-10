package br.senac.recursos.erro;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

/**
 * A fábrica de erro dos filtros de segurança (spec T-22, CA-10; casos TC-25, TC-26 e TC-42).
 * A resposta real do filtro (Content-Type, title e instance) é provada pela T-15.
 */
class FabricaDeProblemasTest {

    @Test
    void tc25_aFabricaCriaOProblemaDe401() {
        ProblemDetail problema = FabricaDeProblemas.autenticacaoNecessaria("/api/x");

        assertThat(problema.getStatus()).isEqualTo(401);
        assertThat(problema.getTitle()).isEqualTo("Autenticação necessária");
        TermosProibidos.naoTem(String.valueOf(problema.getDetail()) + problema.getTitle());
    }

    @Test
    void tc26_aFabricaCriaOProblemaDe403() {
        ProblemDetail problema = FabricaDeProblemas.acessoNegado("/api/x");

        assertThat(problema.getStatus()).isEqualTo(403);
        assertThat(problema.getTitle()).isEqualTo("Acesso negado");
        TermosProibidos.naoTem(String.valueOf(problema.getDetail()) + problema.getTitle());
    }

    @Test
    void tc42_oProblemaTemTypeTitleStatusDetailEInstance() {
        for (ProblemDetail problema : new ProblemDetail[] {
                FabricaDeProblemas.autenticacaoNecessaria("/api/x"), FabricaDeProblemas.acessoNegado("/api/x")}) {
            assertThat(problema.getType()).isNotNull();
            assertThat(problema.getTitle()).isNotBlank();
            assertThat(problema.getStatus()).isIn(401, 403);
            assertThat(problema.getDetail()).isNotBlank();
            assertThat(problema.getInstance()).isNotNull();
            assertThat(problema.getInstance().toString()).isEqualTo("/api/x");
            TermosProibidos.naoTem(problema.getDetail());
        }
    }
}
