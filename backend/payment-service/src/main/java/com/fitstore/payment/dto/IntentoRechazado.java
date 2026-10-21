package com.fitstore.payment.dto;

import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import java.time.Instant;

/** Un intento de cobro rechazado, con el motivo legible. */
public record IntentoRechazado(Instant fecha, Proveedor proveedor, Medio medio, String motivo) {
}
