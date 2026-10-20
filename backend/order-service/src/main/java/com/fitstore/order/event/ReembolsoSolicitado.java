package com.fitstore.order.event;

import com.fitstore.order.pedidos.model.MotivoReembolso;
import java.math.BigDecimal;
import java.util.UUID;

/** {@code order.reembolso.solicitado}: reembolso postventa; {@code solicitudId} es el id de {@code pedido_reembolso}. */
public record ReembolsoSolicitado(UUID pedidoId, String codigo, UUID solicitudId, BigDecimal monto,
        MotivoReembolso motivo) {
}
