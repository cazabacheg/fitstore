package com.fitstore.payment.dto;

import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** Resultado de un cobro (contrato {@code ResultadoCargo}). */
@Schema(name = "ResultadoCargo")
public record ResultadoCargoDto(EstadoTransaccion estado, String codigoPedido, BigDecimal monto, Proveedor proveedor,
        Medio medio, String descripcionMedio) {
}
