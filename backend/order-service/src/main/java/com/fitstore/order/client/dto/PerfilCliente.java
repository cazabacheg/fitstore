package com.fitstore.order.client.dto;

import java.util.UUID;

/** Perfil del cliente de auth ({@code GET /clientes/me}); de aquí sale el destinatario del pedido. */
public record PerfilCliente(UUID id, String email, String nombres, String apellidos, String dni, String telefono) {
}
