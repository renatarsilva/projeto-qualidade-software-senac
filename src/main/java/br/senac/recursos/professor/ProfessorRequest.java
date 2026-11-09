package br.senac.recursos.professor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record ProfessorRequest(
        @NotBlank(message = "deve ser informado")
        @Pattern(regexp = "^[a-z]+-[a-z]+-[0-9]{5}$",
                message = "deve seguir o padrão primeironome-ultimonome-matricula")
        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        String nome,

        Boolean restrito,

        List<String> competencias) {

    private static final int MAXIMO_DA_COMPETENCIA = 80;

    /** Tira os espaços das pontas de cada competência e reduz os repetidos do meio a um só (P-02). */
    public ProfessorRequest limpo() {
        if (competencias == null) {
            return this;
        }
        List<String> limpas = new ArrayList<>();
        for (String competencia : competencias) {
            limpas.add(competencia == null ? null : competencia.trim().replaceAll("\\s+", " "));
        }
        return new ProfessorRequest(nome, restrito, limpas);
    }

    /** Motivos pelos quais a lista de competências é inválida. Use o pedido já limpo. Lista ausente é vista como vazia. */
    public List<String> motivosDasCompetencias() {
        List<String> motivos = new ArrayList<>();
        if (competencias == null) {
            return motivos;
        }
        Set<String> vistas = new HashSet<>();
        boolean repetida = false;
        for (String competencia : competencias) {
            if (competencia == null || competencia.isBlank()) {
                add(motivos, "não pode ser vazia");
            } else {
                if (competencia.length() > MAXIMO_DA_COMPETENCIA) {
                    add(motivos, "deve ter no máximo 80 caracteres");
                }
                if (!vistas.add(competencia.toLowerCase(Locale.ROOT))) {
                    repetida = true;
                }
            }
        }
        if (repetida) {
            add(motivos, "não pode se repetir");
        }
        return motivos;
    }

    private static void add(List<String> motivos, String motivo) {
        if (!motivos.contains(motivo)) {
            motivos.add(motivo);
        }
    }

    /** A matrícula é a terceira parte do nome (P-06). */
    public String matricula() {
        return nome.substring(nome.lastIndexOf('-') + 1);
    }
}
