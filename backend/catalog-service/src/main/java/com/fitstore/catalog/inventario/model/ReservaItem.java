package com.fitstore.catalog.inventario.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Línea de una reserva: cuántas unidades de una variante. */
@Entity
@Table(name = "reserva_item")
@IdClass(ReservaItemId.class)
@Getter
@Setter
public class ReservaItem {
    @Id
    @Column(name = "pedido_id")
    private UUID pedidoId;

    @Id
    @Column(name = "variante_id")
    private Long varianteId;

    @Column(nullable = false)
    private int cantidad;
}
