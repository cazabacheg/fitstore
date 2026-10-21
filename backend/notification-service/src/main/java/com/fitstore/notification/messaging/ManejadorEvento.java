package com.fitstore.notification.messaging;

import java.util.UUID;

/**
 * Un manejador por tipo de evento (SPEC-7 N2): decide qué correos genera. Corre dentro de la transacción del consumidor
 * idempotente, que marca el evento como procesado junto con los envíos que este manejador cree.
 *
 * @param <T> los {@code datos} del evento, tal como los publica su servicio (AsyncAPI)
 */
public interface ManejadorEvento<T> {

    /** Nombre del evento en el sobre ({@code tipo}), por ejemplo {@code BoletaEmitida}. */
    String tipo();

    Class<T> clase();

    void manejar(UUID eventoId, T datos);
}
