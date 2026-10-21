package com.fitstore.payment.dto;

import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detalle de un pago (contrato {@code PagoDetalle}: el resumen más intentos, reembolsos, boleta y montos). Nunca lleva
 * datos de tarjeta ni la respuesta cruda de la pasarela.
 */
public record PagoDetalle(UUID id, String codigoPedido, String clienteNombre, BigDecimal monto, Proveedor proveedor,
        Medio medio, int intentos, EstadoIntencion estado, Instant creadoEn, boolean tieneReembolsoFallido,
        UUID pedidoId, ClientePago cliente, List<TransaccionDto> transacciones, List<ReembolsoDto> reembolsos,
        BoletaInfo boleta, BigDecimal montoReembolsado, BigDecimal neto) {
}
