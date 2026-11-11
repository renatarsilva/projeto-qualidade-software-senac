package br.senac.recursos.manutencao;

import java.time.Instant;

/** Corpo do registro de manutenção. */
public record ManutencaoRequest(Instant inicio, Instant termino, String motivo) {
}
