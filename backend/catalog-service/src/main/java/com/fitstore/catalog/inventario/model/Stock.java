package com.fitstore.catalog.inventario.model;

import jakarta.persistence.*;
import java.time.Instant;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Getter;
import lombok.Setter;

/** Stock de una variante: lo que hay en almacén y lo apartado por pedidos sin pagar. */
@Entity
@Table(name = "stock")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Stock {
    /** Relación 1:1 con la variante, por id (inventario no importa entidades de catálogo). */
    @Id
    @Column(name = "variante_id")
    private Long varianteId;

    @Column(nullable = false)
    private int fisico;

    @Column(nullable = false)
    private int reservado;

    /** NULL = usa {@code configuracion_inventario.stock_minimo_por_defecto}. */
    private Integer minimo;

    @LastModifiedDate
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Version
    private Integer version;

    /** Disponible = físico − reservado (D7). */
    public int disponible() {
        return fisico - reservado;
    }
}
