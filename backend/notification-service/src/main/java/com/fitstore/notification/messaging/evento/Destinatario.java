package com.fitstore.notification.messaging.evento;

/** Destinatario de un pedido. Trae el DNI porque viaja en el evento; los correos nunca lo muestran (micro 2.2). */
public record Destinatario(String nombres, String apellidos, String dni, String telefono, String email) {
}
