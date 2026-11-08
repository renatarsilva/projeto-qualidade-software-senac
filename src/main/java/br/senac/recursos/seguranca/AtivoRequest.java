package br.senac.recursos.seguranca;

import jakarta.validation.constraints.NotNull;

public record AtivoRequest(@NotNull(message = "deve ser informado") Boolean ativo) {
}
