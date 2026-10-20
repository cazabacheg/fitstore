package com.fitstore.order.event;

import java.time.LocalDate;
import java.util.UUID;

/** {@code delivery.entrega.actualizada} (consume order): pasa el pedido a ENVIADO, ENTREGADO o DEVUELTO. */
public record EntregaActualizada(UUID entregaId, UUID pedidoId, String codigoPedido, String modalidad, String estado,
        int intento, LocalDate fechaProgramada, String repartidorNombre, String agenciaNombre,
        String codigoSeguimiento, String claveRecojo, String urlSeguimiento, String observacion,
        PersonaEvento destinatario) {
}
