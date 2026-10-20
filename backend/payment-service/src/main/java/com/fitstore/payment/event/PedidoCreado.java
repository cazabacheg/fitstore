package com.fitstore.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Payload de {@code order.pedido.creado}, que payment consume para crear la intención de pago (T2). */
public record PedidoCreado(UUID pedidoId, String codigo, UUID clienteId, Destinatario destinatario,
        ModalidadEntrega modalidad, Direccion direccion, List<ItemPedido> items, BigDecimal subtotal,
        BigDecimal descuentoTotal, BigDecimal costoEnvio, BigDecimal total, Instant expiraEn) {

    /** Quien recibe el pedido (esquema {@code Destinatario}). */
    public record Destinatario(String nombres, String apellidos, String dni, String telefono, String email) {
    }

    /** Dirección de entrega; no viene en recojo (esquema {@code Direccion}). */
    public record Direccion(String ubigeo, String departamento, String provincia, String distrito, String direccion,
            String referencia) {
    }

    /** Ítem con su precio y descuento (esquema {@code ItemPedido}). */
    public record ItemPedido(Long varianteId, Long productoId, String sku, String producto, String talla, String color,
            String imagenUrl, BigDecimal precioUnitario, int cantidad, BigDecimal descuentoPorcentaje,
            BigDecimal descuentoMonto, BigDecimal total) {
    }
}
