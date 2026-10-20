package com.fitstore.order.pedidos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una fila por cada cambio de estado del pedido (invariante 4); la línea de tiempo que ve el cliente. */
@Entity
@Table(name = "pedido_historial")
@Getter
@Setter
@NoArgsConstructor
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    /** Nulo en la fila inicial, cuando el pedido se crea. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoPedido estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoPedido estadoNuevo;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_tipo", nullable = false, length = 10)
    private TipoActor actorTipo;

    /** Nulo cuando el actor es el sistema. */
    @Column(name = "actor_id")
    private UUID actorId;

    @Column(length = 500)
    private String comentario;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @PrePersist
    void antesDeGuardar() {
        if (creadoEn == null) {
            creadoEn = Instant.now();
        }
    }
}
