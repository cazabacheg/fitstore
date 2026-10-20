package com.fitstore.order.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** {@code payment.pago.confirmado} (consume order): pasa el pedido a PAGADO y guarda proveedor, medio y fecha. */
public record PagoConfirmado(UUID pedidoId, UUID intencionId, UUID transaccionId, String proveedor, String medio,
        BigDecimal monto, Instant confirmadoEn) {
}
