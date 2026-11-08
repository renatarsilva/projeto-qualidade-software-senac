package br.senac.recursos.seguranca;

import java.time.Instant;

public record LoginResponse(String token, String tipo, Instant expiraEm) {
}
