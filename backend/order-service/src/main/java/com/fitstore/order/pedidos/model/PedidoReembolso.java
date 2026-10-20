package com.fitstore.order.pedidos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Reembolso postventa de un pedido entregado (micro order 4.8). Su {@code id} es la {@code solicitudId} del evento. */
@Entity
@Table(name = "pedido_reembolso")
@Getter
@Setter
@NoArgsConstructor
public class PedidoReembolso {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "pedido_id", nullable = false)
    private UUID pedidoId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MotivoReembolso motivo;

    @Column(length = 500)
    private String nota;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<ItemReembolso> items;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoReembolso estado = EstadoReembolso.SOLICITADO;

    @Column(name = "solicitado_por", nullable = false)
    private UUID solicitadoPor;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "completado_en")
    private Instant completadoEn;

    @PrePersist
    void antesDeGuardar() {
        if (creadoEn == null) {
            creadoEn = Instant.now();
        }
    }
}
