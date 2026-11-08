package br.senac.recursos.professor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Regras das competências, sem subir a aplicação (spec T-17, CA-03; casos TC-05, TC-06, TC-07, TC-35, TC-36, TC-37).
 * Cada competência é limpa (P-02) antes de validar: espaços das pontas saem e os repetidos do meio viram um só.
 */
class CompetenciasDoProfessorTest {

    private ProfessorRequest comCompetencias(String... competencias) {
        return new ProfessorRequest("renata-silva-12345", false, List.of(competencias)).limpo();
    }

    @Test
    void tc05_competenciaEmBrancoERecusada() {
        assertThat(comCompetencias("Java", "").motivosDasCompetencias()).isNotEmpty();
    }

    @Test
    void tc06_oitentaCaracteresSaoAceitosEOitentaEUmSaoRecusados() {
        assertThat(comCompetencias("c".repeat(80)).motivosDasCompetencias()).isEmpty();
        assertThat(comCompetencias("c".repeat(81)).motivosDasCompetencias()).isNotEmpty();
    }

    @Test
    void tc07_javaEJavaNaMesmaListaSaoRecusadas() {
        assertThat(comCompetencias("Java", "java").motivosDasCompetencias()).isNotEmpty();
    }

    @Test
    void tc35_aLimpezaDevolveBancoDeDadosSemOsEspacosExtras() {
        ProfessorRequest limpo = comCompetencias("  Banco   de Dados ");

        assertThat(limpo.competencias()).containsExactly("Banco de Dados");
        assertThat(limpo.motivosDasCompetencias()).isEmpty();
    }

    @Test
    void tc36_javaComEspacosECompetenciaRepetidaDepoisDaLimpeza() {
        assertThat(comCompetencias(" Java", "java ").motivosDasCompetencias()).isNotEmpty();
    }

    @Test
    void tc37_soEspacosERecusadoEOitentaCaracteresComEspacosNasPontasSaoAceitos() {
        assertThat(comCompetencias("     ").motivosDasCompetencias()).isNotEmpty();
        assertThat(comCompetencias("  " + "c".repeat(80) + "  ").motivosDasCompetencias()).isEmpty();
    }
}
