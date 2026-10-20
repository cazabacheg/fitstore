package com.fitstore.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Webhook de una pasarela, guardado antes de procesarlo; el par (proveedor, evento) es único (idempotencia). */
@Entity
@Table(name = "webhook_recibido")
@Getter
@Setter
public class WebhookRecibido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Proveedor proveedor;

    @Column(name = "evento_id_proveedor", nullable = false, length = 100)
    private String eventoIdProveedor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "recibido_en", nullable = false, updatable = false)
    private Instant recibidoEn;

    @Column(name = "procesado_en")
    private Instant procesadoEn;

    @PrePersist
    void alCrear() {
        if (recibidoEn == null) {
            recibidoEn = Instant.now();
        }
    }
}
