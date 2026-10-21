package com.fitstore.order.pedidos.dto;

import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.TipoActor;
import java.time.Instant;

/**
 * Una fila de la línea de tiempo (schema {@code HistorialEntrada}). {@code tipo} es CAMBIO_ESTADO, PAGO_RECHAZADO,
 * INTENTO_FALLIDO, REEMBOLSO_SOLICITADO o REEMBOLSO_COMPLETADO; {@code actorNombre} solo se llena en el backoffice.
 */
public record HistorialEntradaDto(Instant fecha, String tipo, EstadoPedido estadoNuevo, String comentario,
        TipoActor actorTipo, String actorNombre) {
}
