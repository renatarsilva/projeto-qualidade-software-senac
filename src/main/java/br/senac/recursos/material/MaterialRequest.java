package br.senac.recursos.material;

public record MaterialRequest(String nome, String tipoMaterial, String patrimonio, Boolean restrito) {

    /** Limpa os espaços do nome, do tipo e do patrimônio (P-07). Neutro por enquanto: devolve o pedido como veio. */
    public MaterialRequest limpo() {
        return this;
    }
}
