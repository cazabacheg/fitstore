package com.fitstore.catalog.inventario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Parámetros de inventario; los edita el backoffice pero el dato es de catalog. */
@Entity
@Table(name = "configuracion_inventario")
@Getter
@Setter
public class ConfiguracionInventario {
    /** Fila única: siempre 1. */
    @Id
    private Long id;

    /** De 0 a 999. */
    @Column(name = "stock_minimo_por_defecto", nullable = false)
    private int stockMinimoPorDefecto;

    /** De 0 a 30 minutos que se suman a la expiración del pedido (micro 4.6). */
    @Column(name = "margen_reserva_minutos", nullable = false)
    private int margenReservaMinutos;
}
