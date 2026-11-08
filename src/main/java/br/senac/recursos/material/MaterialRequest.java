package br.senac.recursos.material;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MaterialRequest(
        @NotBlank(message = "deve ser informado")
        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        String nome,

        @NotBlank(message = "deve ser informado")
        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        String tipoMaterial,

        @NotBlank(message = "deve ser informado")
        @Size(max = 30, message = "deve ter no máximo 30 caracteres")
        String patrimonio,

        Boolean restrito) {

    /** Tira os espaços das pontas e reduz os repetidos do meio a um só, no nome, no tipo e no patrimônio (P-07). */
    public MaterialRequest limpo() {
        return new MaterialRequest(limpa(nome), limpa(tipoMaterial), limpa(patrimonio), restrito);
    }

    private static String limpa(String texto) {
        return texto == null ? null : texto.trim().replaceAll("\\s+", " ");
    }
}
