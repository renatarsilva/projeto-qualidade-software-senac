package br.senac.recursos.seguranca;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "deve ser informado")
        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        String nome,

        @NotBlank(message = "deve ser informado")
        @Email(message = "deve ser um e-mail válido")
        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
        String email,

        @NotBlank(message = "deve ser informado")
        String senha,

        @NotNull(message = "deve ser informado")
        Perfil perfil) {
}
