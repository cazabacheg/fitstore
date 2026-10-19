package com.fitstore.auth.event;

import java.util.UUID;

/** Payload (campo {@code datos} del sobre) de {@code auth.cliente.registrado}. Lo consume notification (bienvenida). */
public record ClienteRegistrado(UUID clienteId, String email, String nombres) {
}
