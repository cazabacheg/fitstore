package com.fitstore.order.pedidos.dto;

/** Quién recibe el pedido (schema {@code Destinatario}). */
public record DestinatarioDto(String nombres, String apellidos, String dni, String telefono, String email) {
}
