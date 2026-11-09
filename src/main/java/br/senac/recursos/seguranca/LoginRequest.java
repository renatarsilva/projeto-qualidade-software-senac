package br.senac.recursos.seguranca;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "deve ser informado") String email,
        @NotBlank(message = "deve ser informado") String senha) {
}
