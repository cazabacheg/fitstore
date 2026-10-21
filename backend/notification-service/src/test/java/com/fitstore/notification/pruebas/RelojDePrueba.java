package com.fitstore.notification.pruebas;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj que solo avanza cuando la prueba lo pide (ritmo y reintentos sin esperar de verdad). */
public class RelojDePrueba extends Clock {

    private volatile Instant ahora = Instant.parse("2026-10-22T15:00:00Z");

    public void fijar(Instant instante) {
        this.ahora = instante;
    }

    public void avanzar(Duration tiempo) {
        this.ahora = ahora.plus(tiempo);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }

    @Override
    public Instant instant() {
        return ahora;
    }
}
