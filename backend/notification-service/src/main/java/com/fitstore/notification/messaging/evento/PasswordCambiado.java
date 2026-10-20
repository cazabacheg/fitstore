package com.fitstore.notification.messaging.evento;

import java.util.UUID;

/** Datos del evento {@code PasswordCambiado} (routing key {@code auth.password.cambiado}), según el AsyncAPI. */
public record PasswordCambiado(UUID cuentaId, String tipoCuenta, String email, String nombres) {
}
