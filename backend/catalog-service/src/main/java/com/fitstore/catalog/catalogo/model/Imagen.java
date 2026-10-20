package com.fitstore.catalog.catalogo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Imagen de un producto. */
@Entity
@Table(name = "imagen")
@Getter
@Setter
public class Imagen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    /** Cada imagen pertenece a un color del producto (D3); NULL mientras no se asigne. */
    @Column(name = "color_id")
    private Long colorId;

    /** Ruta en el almacenamiento: {@code productos/{productoId}/{uuid}-{tamaño}.jpg}. */
    @Column(nullable = false, length = 255)
    private String clave;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean principal;

    private Integer ancho;

    private Integer alto;
}
