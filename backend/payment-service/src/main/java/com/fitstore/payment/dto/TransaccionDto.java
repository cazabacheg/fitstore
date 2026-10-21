package com.fitstore.payment.dto;

import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Un intento de cobro tal como lo ve el administrador (contrato {@code Transaccion}). */
@Schema(name = "Transaccion")
public record TransaccionDto(UUID id, Proveedor proveedor, Medio medio, EstadoTransaccion estado, BigDecimal monto,
        String idProveedor, String codigoRespuesta, String mensaje, Instant creadoEn) {
}
