package com.fitstore.payment.dto;

import java.math.BigDecimal;

/** Una línea del resumen de la página de pago. */
public record ItemPago(String producto, String talla, String color, int cantidad, BigDecimal total) {
}
