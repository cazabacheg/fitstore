package com.fitstore.catalog.catalogo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Interruptor global de descuentos. */
@Entity
@Table(name = "configuracion_descuento")
@Getter
@Setter
public class ConfiguracionDescuento {
    /** Fila única: siempre 1. */
    @Id
    private Long id;

    @Column(nullable = false)
    private boolean activo;
}
