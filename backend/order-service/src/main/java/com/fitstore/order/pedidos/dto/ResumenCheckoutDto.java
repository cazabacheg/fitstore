package com.fitstore.order.pedidos.dto;

import java.math.BigDecimal;
import java.util.List;

/** Resumen del pedido antes de confirmar (schema {@code ResumenCheckout}); no crea nada. */
public record ResumenCheckoutDto(List<ItemPedidoDto> items, BigDecimal subtotal, BigDecimal descuentoTotal,
        BigDecimal costoEnvio, BigDecimal total, EnvioDto envio, DireccionPedidoDto direccion,
        int expiracionMinutos) {
}
