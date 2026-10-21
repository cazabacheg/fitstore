package com.fitstore.payment.service;

import com.fitstore.payment.event.PedidoCreado;
import java.util.UUID;

/** Ciclo de vida de la intención de pago a partir de los eventos de order (T2, HU-5-01). */
public interface IntencionPagoService {

    /** Crea la intención PENDIENTE del pedido; idempotente por {@code pedidoId} (CA-5-01). */
    void crear(PedidoCreado pedido);

    /** Anula la intención de un pedido expirado o cancelado sin pago; no toca una ya pagada (CA-5-02). */
    void anular(UUID pedidoId);
}
