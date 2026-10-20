package com.fitstore.notification.messaging.evento;

/** Dirección de entrega de un pedido. */
public record Direccion(String ubigeo, String departamento, String provincia, String distrito, String direccion, String referencia) {
}
