package com.fitstore.order.carrito.dto;

import java.math.BigDecimal;

/** Tramo de descuento alcanzado y cuánto falta para el siguiente ("Agrega 2 más y ahorra 15 %", CA-4-02). */
public record AvisoTramoDto(Long productoId, String producto, int unidades, Tramo tramoAlcanzado,
        SiguienteTramo siguienteTramo) {

    public record Tramo(int unidadesMinimas, BigDecimal porcentaje) {
    }

    public record SiguienteTramo(int unidadesMinimas, BigDecimal porcentaje, int faltan) {
    }
}
