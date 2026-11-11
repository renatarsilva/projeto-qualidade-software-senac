package br.senac.recursos.web;

import br.senac.recursos.erro.EntradaInvalidaException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/** Conversão entre o horário que a pessoa digita (Brasília) e o instante que os serviços usam (UTC). */
final class Fuso {

    static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    private Fuso() {
    }

    /** "2026-11-10T10:00" (campo datetime-local) em Brasília vira o instante. Vazio vira null. */
    static Instant instante(String campo, String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(texto.trim()).atZone(BRASILIA).toInstant();
        } catch (DateTimeParseException erro) {
            throw invalido(campo, "data e hora inválidas");
        }
    }

    /** Primeiro instante do dia (campo date) em Brasília. */
    static Instant inicioDoDia(String campo, String texto) {
        return dia(campo, texto).atStartOfDay(BRASILIA).toInstant();
    }

    /** Fim do dia inclusive: o início do dia seguinte. */
    static Instant fimDoDia(String campo, String texto) {
        return dia(campo, texto).plusDays(1).atStartOfDay(BRASILIA).toInstant();
    }

    private static LocalDate dia(String campo, String texto) {
        if (texto == null || texto.isBlank()) {
            throw invalido(campo, "deve ser informado");
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException erro) {
            throw invalido(campo, "data inválida");
        }
    }

    private static EntradaInvalidaException invalido(String campo, String motivo) {
        return new EntradaInvalidaException(List.of(Map.of("campo", campo, "motivo", motivo)));
    }
}
