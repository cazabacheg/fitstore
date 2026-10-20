package com.fitstore.catalog.catalogo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Tramo de descuento por volumen (global o de un producto). */
@Entity
@Table(name = "tramo_descuento")
@Getter
@Setter
public class TramoDescuento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NULL = política global. */
    @Column(name = "producto_id")
    private Long productoId;

    @Column(name = "unidades_minimas", nullable = false)
    private int unidadesMinimas;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;
}
