package com.fitstore.order.event;

import java.util.UUID;

/** {@code payment.boleta.emitida} (consume order): guarda la URL de la boleta en {@code pedido.boleta_url}. */
public record BoletaEmitida(UUID pedidoId, String numero, String url) {
}
