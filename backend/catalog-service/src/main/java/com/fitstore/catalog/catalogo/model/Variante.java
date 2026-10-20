package com.fitstore.catalog.catalogo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Variante = producto × talla × color, con SKU único. */
@Entity
@Table(name = "variante")
@Getter
@Setter
public class Variante extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "talla_id", nullable = false)
    private Long tallaId;

    @Column(name = "color_id", nullable = false)
    private Long colorId;

    @Column(nullable = false, length = 60)
    private String sku;

    /** NULL = usa el precio base del producto. */
    @Column(precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private boolean activo = true;
}
