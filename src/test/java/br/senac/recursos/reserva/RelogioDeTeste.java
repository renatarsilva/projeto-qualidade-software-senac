package br.senac.recursos.reserva;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Clock dos testes de API: fixo em segunda, 2026-11-09, 08:00 UTC, e o teste pode avançá-lo. */
public class RelogioDeTeste extends Clock {

    private volatile Instant agora = HorariosDeTeste.AGORA;

    void defineAgora(Instant instante) {
        this.agora = instante;
    }

    void volta() {
        this.agora = HorariosDeTeste.AGORA;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return agora;
    }
}
