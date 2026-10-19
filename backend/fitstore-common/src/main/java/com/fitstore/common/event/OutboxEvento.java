package com.fitstore.common.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Fila de {@code outbox_evento} (ADR-009). La tabla la crea la migración Flyway de cada servicio. */
@Entity
@Table(name = "outbox_evento")
public class OutboxEvento {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(name = "agregado_id", nullable = false, length = 64)
    private String agregadoId;

    /** El contenido {@code datos} del sobre, como JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "publicado_en")
    private Instant publicadoEn;

    protected OutboxEvento() {
    }

    public static OutboxEvento nuevo(String tipo, String agregadoId, String payloadJson) {
        OutboxEvento evento = new OutboxEvento();
        evento.id = UUID.randomUUID();
        evento.tipo = tipo;
        evento.agregadoId = agregadoId;
        evento.payload = payloadJson;
        evento.creadoEn = Instant.now();
        return evento;
    }

    public UUID getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getAgregadoId() {
        return agregadoId;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getPublicadoEn() {
        return publicadoEn;
    }
}
