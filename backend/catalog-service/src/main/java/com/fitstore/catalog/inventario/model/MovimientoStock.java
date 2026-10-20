package com.fitstore.catalog.inventario.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Fila del kardex: todo cambio de físico deja un movimiento con usuario, motivo y saldo resultante. */
@Entity
@Table(name = "movimiento_stock")
@Getter
@Setter
public class MovimientoStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "variante_id", nullable = false)
    private Long varianteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    /** Con signo. */
    @Column(nullable = false)
    private int cantidad;

    @Column(name = "saldo_fisico", nullable = false)
    private int saldoFisico;

    private String motivo;

    /** NULL = sistema. */
    @Column(name = "usuario_id")
    private UUID usuarioId;

    /** Código de pedido u otra referencia. */
    private String referencia;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    void alCrear() {
        if (creadoEn == null) {
            creadoEn = Instant.now();
        }
    }
}
