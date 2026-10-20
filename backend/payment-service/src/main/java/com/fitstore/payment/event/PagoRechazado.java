package com.fitstore.payment.event;

import com.fitstore.payment.model.Proveedor;
import java.util.UUID;

/** Payload de {@code payment.pago.rechazado}. Lo consume order (historial). {@code motivo} es legible. */
public record PagoRechazado(UUID pedidoId, Proveedor proveedor, String motivo) {
}
