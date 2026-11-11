package br.senac.recursos.erro;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A lista única de termos que nenhuma resposta de erro pode conter (spec T-22, RNF-08).
 * A busca ignora maiúsculas. Toda resposta de erro dos testes passa por aqui.
 */
public final class TermosProibidos {

    private static final List<String> TERMOS = List.of("exception", "select", "insert", "update", "delete from",
            "org.postgresql", "org.springframework", "org.hibernate", "com.fasterxml", "br.senac.recursos", "java.",
            "duplicate key", "violates", "constraint", "/src/", ".java", "stack");

    /** Rastro de pilha: " at nome.da.Classe(Arquivo.java:12)". */
    private static final Pattern RASTRO = Pattern.compile("\\sat\\s+[\\w.$]+\\([\\w$]+\\.java:\\d+\\)");

    private TermosProibidos() {
    }

    public static void naoTem(String corpo) {
        String minusculo = corpo.toLowerCase();
        for (String termo : TERMOS) {
            assertThat(minusculo).as("a resposta não pode conter o termo \"%s\"", termo).doesNotContain(termo);
        }
        assertThat(RASTRO.matcher(corpo).find()).as("a resposta não pode conter rastro de pilha").isFalse();
    }
}
