package com.fitstore.payment.event;

import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Payload de {@code payment.pago.confirmado}. Lo consume order (pasa el pedido a PAGADO y guarda el medio). */
public record PagoConfirmado(UUID pedidoId, UUID intencionId, UUID transaccionId, Proveedor proveedor, Medio medio,
        BigDecimal monto, Instant confirmadoEn) {
}
