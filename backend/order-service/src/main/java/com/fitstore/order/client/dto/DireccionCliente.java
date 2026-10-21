package com.fitstore.order.client.dto;

/** Dirección de un cliente de auth ({@code GET /clientes/me/direcciones/{id}}). */
public record DireccionCliente(Long id, String alias, String ubigeo, String direccion, String referencia,
        String telefonoContacto, boolean predeterminada) {
}
