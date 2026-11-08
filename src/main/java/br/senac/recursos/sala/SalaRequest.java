package br.senac.recursos.sala;

import jakarta.validation.constraints.Min;

public record SalaRequest(
        String nome,
        String tipoSala,
        @Min(value = 1, message = "deve ser maior que zero") Integer capacidade,
        String localizacao,
        Boolean restrito) {

    /** Remove espaços extras do tipo e da localização (P-05). Neutro por enquanto: devolve o pedido como veio. */
    public SalaRequest limpo() {
        return this;
    }
}
