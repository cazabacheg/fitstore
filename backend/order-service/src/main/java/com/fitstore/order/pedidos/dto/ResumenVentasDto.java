package com.fitstore.order.pedidos.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Resumen de ventas del dashboard (extensión de alcance, HU-4-11): solo declara la forma del contrato. */
public record ResumenVentasDto(VentasPeriodoDto hoy, VentasPeriodoDto semana, VentasPeriodoDto mes,
        BigDecimal reembolsadoMes, Map<String, Long> porEstado, List<TopProductoDto> topProductos, Instant generadoEn) {

    public record VentasPeriodoDto(BigDecimal monto, long cantidad) {
    }

    public record TopProductoDto(Long productoId, String nombre, long unidades) {
    }
}
