package com.fitstore.order.pedidos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Montos del pedido (D8, CA-4-25): PEN con IGV, 2 decimales, redondeo half-up.
 * {@code total = subtotal − descuentoTotal + costoEnvio}, y cada ítem cuadra igual.
 */
public final class MontosPedido {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private MontosPedido() {
    }

    /** Una línea: precio unitario, unidades y descuento ya calculado (lo da catalog). */
    public record Item(BigDecimal precioUnitario, int cantidad, BigDecimal descuentoMonto) {
    }

    public record Montos(BigDecimal subtotal, BigDecimal descuentoTotal, BigDecimal costoEnvio, BigDecimal total) {
    }

    /** Precio por unidades, sin descuento. */
    public static BigDecimal bruto(Item item) {
        return escala(item.precioUnitario().multiply(BigDecimal.valueOf(item.cantidad())));
    }

    /** Total de la línea: precio × unidades − descuento. */
    public static BigDecimal totalItem(Item item) {
        return bruto(item).subtract(escala(item.descuentoMonto()));
    }

    /** Descuento de una línea según el porcentaje del tramo alcanzado (half-up a 2 decimales). */
    public static BigDecimal descuento(BigDecimal precioUnitario, int cantidad, BigDecimal porcentaje) {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad)).multiply(porcentaje)
                .divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    public static Montos calcular(List<Item> items, BigDecimal costoEnvio) {
        BigDecimal subtotal = items.stream().map(MontosPedido::bruto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descuento = items.stream().map(i -> escala(i.descuentoMonto())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal envio = escala(costoEnvio);
        return new Montos(escala(subtotal), escala(descuento), envio,
                escala(subtotal.subtract(descuento).add(envio)));
    }

    private static BigDecimal escala(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
