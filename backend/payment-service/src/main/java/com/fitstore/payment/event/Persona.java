package com.fitstore.payment.event;

/** Nombre y correo de quien recibe un aviso (esquema {@code Persona} del AsyncAPI). */
public record Persona(String nombre, String email) {
}
