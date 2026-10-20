package com.fitstore.order.event;

/** Destinatario dentro de los eventos (AsyncAPI {@code Destinatario}). */
public record DestinatarioEvento(String nombres, String apellidos, String dni, String telefono, String email) {
}
