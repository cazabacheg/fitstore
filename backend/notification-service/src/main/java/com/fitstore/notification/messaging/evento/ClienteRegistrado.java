package com.fitstore.notification.messaging.evento;

import java.util.UUID;

/** Datos del evento {@code ClienteRegistrado} (routing key {@code auth.cliente.registrado}), según el AsyncAPI. */
public record ClienteRegistrado(UUID clienteId, String email, String nombres) {
}
