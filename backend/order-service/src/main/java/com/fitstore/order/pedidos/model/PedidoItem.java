package com.fitstore.order.pedidos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Línea del pedido: copia congelada del producto, la variante y el precio al confirmar (D3). */
@Entity
@Table(name = "pedido_item")
@Getter
@Setter
@NoArgsConstructor
public class PedidoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "variante_id", nullable = false)
    private Long varianteId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(nullable = false, length = 60)
    private String sku;

    @Column(name = "producto_nombre", nullable = false, length = 200)
    private String productoNombre;

    @Column(nullable = false, length = 20)
    private String talla;

    @Column(nullable = false, length = 40)
    private String color;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "descuento_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal descuentoPorcentaje = BigDecimal.ZERO;

    @Column(name = "descuento_monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal descuentoMonto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;
}
