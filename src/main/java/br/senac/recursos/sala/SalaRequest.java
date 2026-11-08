package br.senac.recursos.sala;

import jakarta.validation.constraints.Min;

public record SalaRequest(
        String nome,
        String tipoSala,
        @Min(value = 1, message = "deve ser maior que zero") Integer capacidade,
        String localizacao,
        Boolean restrito) {
}
