package br.senac.recursos.retirada;

import java.time.Instant;

/** Uma retirada de material. {@code atrasada}: devolvida ou, sem devolução, consultada depois do término da reserva. */
public record RetiradaResponse(long id, long reservaId, long materialId, long registradaPor, Instant retiradaEm,
        Instant devolvidaEm, boolean atrasada) {
}
