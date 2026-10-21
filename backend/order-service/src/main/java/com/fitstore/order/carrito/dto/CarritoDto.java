package com.fitstore.order.carrito.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Carrito cotizado (schema {@code Carrito} del contrato). {@code preciosPorConfirmar} es verdadero cuando catalog no
 * respondió y se muestra la última cotización (CA-4-07).
 */
public record CarritoDto(List<LineaCarritoDto> lineas, List<AvisoTramoDto> avisosTramo, BigDecimal subtotal,
        BigDecimal descuentoTotal, BigDecimal total, int cantidadTotal, boolean preciosPorConfirmar,
        boolean puedeIrAlCheckout, List<String> avisos) {

    public static CarritoDto vacio() {
        return new CarritoDto(List.of(), List.of(), BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2), 0, false, false, List.of());
    }
}
