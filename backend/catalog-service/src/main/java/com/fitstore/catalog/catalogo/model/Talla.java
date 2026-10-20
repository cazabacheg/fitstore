package com.fitstore.catalog.catalogo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Talla maestra (XS, S, M…), administrada por el ADMIN. */
@Entity
@Table(name = "talla")
@Getter
@Setter
public class Talla {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean activo = true;
}
