package br.senac.recursos.reserva;

import java.time.Instant;

/** Instantes dos testes de reserva, todos calculados a partir do Clock fixo (spec T-20, seção 6). */
final class HorariosDeTeste {

    /** Segunda, 2026-11-09, 08:00 UTC. */
    static final Instant AGORA = Instant.parse("2026-11-09T08:00:00Z");

    private HorariosDeTeste() {
    }

    /** Segunda, 2026-11-09. */
    static Instant segunda(int hora, int minuto) {
        return Instant.parse("2026-11-09T00:00:00Z").plusSeconds(hora * 3600L + minuto * 60L);
    }

    /** Terça, 2026-11-10. */
    static Instant terca(int hora, int minuto) {
        return segunda(hora, minuto).plusSeconds(24 * 3600L);
    }

    /** Quarta, 2026-11-11. */
    static Instant quarta(int hora, int minuto) {
        return segunda(hora, minuto).plusSeconds(48 * 3600L);
    }
}
