package br.senac.recursos.sala;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SalaRequest(
        @NotBlank(message = "deve ser informado")
        @Pattern(regexp = "^sala-[0-9]{2,3}$", message = "deve seguir o padrão sala-NN")
        String nome,

        @NotBlank(message = "deve ser informado")
        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        String tipoSala,

        @NotNull(message = "deve ser informado")
        @Min(value = 1, message = "deve ser maior que zero")
        Integer capacidade,

        @NotBlank(message = "deve ser informado")
        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        String localizacao,

        Boolean restrito) {

    /** Tira os espaços das pontas e reduz os repetidos do meio a um só, no tipo e na localização (P-05). */
    public SalaRequest limpo() {
        return new SalaRequest(nome, limpa(tipoSala), capacidade, limpa(localizacao), restrito);
    }

    private static String limpa(String texto) {
        return texto == null ? null : texto.trim().replaceAll("\\s+", " ");
    }
}
