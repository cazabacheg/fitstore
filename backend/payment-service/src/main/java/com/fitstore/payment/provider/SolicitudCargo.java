package com.fitstore.payment.provider;

import com.fitstore.payment.model.Medio;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que el adaptador necesita para cobrar. El monto sale siempre de la intención, nunca del navegador.
 *
 * @param referencia       id de la transacción interna; viaja a la pasarela para poder reconocer el cargo después
 * @param idempotencyKey   la clave del navegador, que también se envía a la pasarela
 * @param datosAdicionales campos que pide el Payment Brick (método, cuotas, emisor); el adaptador usa solo los que conoce
 */
public record SolicitudCargo(UUID intencionId, String referencia, String codigoPedido, BigDecimal monto, String token,
        Medio medio, String email, String idempotencyKey, Map<String, Object> datosAdicionales) {
}
