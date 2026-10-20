package com.fitstore.delivery.event;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento que publica delivery en cada cambio de estado ({@code delivery.entrega.actualizada}): contenido {@code datos}
 * del sobre K12. Nunca lleva el teléfono del repartidor (D6).
 */
public record EntregaActualizada(UUID entregaId, UUID pedidoId, String codigoPedido, String modalidad, String estado,
        int intento, LocalDate fechaProgramada, String repartidorNombre, String agenciaNombre,
        String codigoSeguimiento, String claveRecojo, String urlSeguimiento, String observacion, Persona destinatario) {

    public record Persona(String nombre, String email) {
    }
}
