package com.fitstore.payment.dto;

import com.fitstore.payment.model.EstadoReembolso;
import com.fitstore.payment.model.MotivoReembolso;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Un reembolso de un pago (contrato {@code Reembolso}). */
@Schema(name = "Reembolso")
public record ReembolsoDto(UUID id, UUID intencionId, String codigoPedido, String clienteNombre, BigDecimal monto,
        MotivoReembolso motivo, EstadoReembolso estado, int intentos, String ultimoError, Instant proximoIntentoEn,
        String resueltoPor, String nota, Instant creadoEn) {
}
