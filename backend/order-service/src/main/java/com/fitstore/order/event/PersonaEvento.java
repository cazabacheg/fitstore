package com.fitstore.order.event;

/** Nombre y correo de una persona dentro de los eventos que order consume (AsyncAPI {@code Persona}). */
public record PersonaEvento(String nombre, String email) {
}
