package com.fitstore.notification.messaging.evento;

import java.time.LocalDate;
import java.util.UUID;

/** Datos del evento {@code EntregaActualizada} (routing key {@code delivery.entrega.actualizada}), según el AsyncAPI. */
public record EntregaActualizada(UUID entregaId, UUID pedidoId, String codigoPedido, String modalidad, String estado,
        int intento, LocalDate fechaProgramada, String repartidorNombre, String agenciaNombre,
        String codigoSeguimiento, String claveRecojo, String urlSeguimiento, String observacion,
        Persona destinatario) {
}
