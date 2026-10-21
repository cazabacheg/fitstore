package com.fitstore.payment.dto;

import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Una fila del listado de pagos del backoffice; proveedor y medio son los del cargo aprobado (nulos si aún no se paga). */
public record PagoResumen(UUID id, String codigoPedido, String clienteNombre, BigDecimal monto, Proveedor proveedor,
        Medio medio, int intentos, EstadoIntencion estado, Instant creadoEn, boolean tieneReembolsoFallido) {
}
