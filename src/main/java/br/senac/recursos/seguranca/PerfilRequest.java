package br.senac.recursos.seguranca;

import jakarta.validation.constraints.NotNull;

public record PerfilRequest(@NotNull(message = "deve ser informado") Perfil perfil) {
}
