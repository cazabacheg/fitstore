package com.fitstore.catalog.inventario.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Getter;
import lombok.Setter;

/** Reserva de stock de un pedido (una por pedido). */
@Entity
@Table(name = "reserva")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Reserva {
    @Id
    @Column(name = "pedido_id")
    private UUID pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @CreatedDate
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @LastModifiedDate
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;
}
