package com.fitstore.catalog.catalogo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Categoría del catálogo (raíz o hija). */
@Entity
@Table(name = "categoria")
@Getter
@Setter
public class Categoria extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NULL en las categorías raíz; el árbol tiene como máximo 2 niveles. */
    @Column(name = "padre_id")
    private Long padreId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean activo = true;
}
