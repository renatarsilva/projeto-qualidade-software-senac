package br.senac.recursos.professor;

import java.util.List;

public record ProfessorRequest(String nome, Boolean restrito, List<String> competencias) {

    /** Limpa os espaços das competências (P-02). Neutro por enquanto: devolve o pedido como veio. */
    public ProfessorRequest limpo() {
        return this;
    }

    /** Motivos pelos quais a lista de competências é inválida. Neutro por enquanto: nunca recusa. */
    public List<String> motivosDasCompetencias() {
        return List.of();
    }
}
