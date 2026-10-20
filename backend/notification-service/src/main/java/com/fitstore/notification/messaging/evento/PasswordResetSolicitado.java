package com.fitstore.notification.messaging.evento;

import java.time.Instant;
import java.util.UUID;

/** Datos del evento {@code PasswordResetSolicitado} (routing key {@code auth.password.reset-solicitado}), según el AsyncAPI. */
public record PasswordResetSolicitado(UUID cuentaId, String tipoCuenta, String email, String nombres, String motivo,
        String enlace, Instant expiraEn) {
}
