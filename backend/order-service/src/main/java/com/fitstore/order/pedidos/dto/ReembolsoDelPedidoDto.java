package com.fitstore.order.pedidos.dto;

import com.fitstore.order.pedidos.model.EstadoReembolso;
import com.fitstore.order.pedidos.model.MotivoReembolso;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Un reembolso postventa ya registrado en el pedido (schema {@code ReembolsoDelPedido}). */
public record ReembolsoDelPedidoDto(UUID id, BigDecimal monto, MotivoReembolso motivo, String nota,
        EstadoReembolso estado, String solicitadoPor, Instant creadoEn, Instant completadoEn) {
}
