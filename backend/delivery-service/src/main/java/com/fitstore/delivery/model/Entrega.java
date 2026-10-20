package com.fitstore.delivery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entrega de un pedido (una por {@code pedidoId}). Guarda copia del destinatario y la dirección (D3). */
@Entity
@Table(name = "entrega")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entrega {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "pedido_id", nullable = false)
    private UUID pedidoId;

    @Column(name = "codigo_pedido", nullable = false, length = 30)
    private String codigoPedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Modalidad modalidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoEntrega estado;

    @Embedded
    private Destinatario destinatario;

    @Embedded
    private DireccionEnvio direccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repartidor_id")
    private Repartidor repartidor;

    @Column(name = "fecha_programada")
    private LocalDate fechaProgramada;

    @Column(nullable = false)
    private int intentos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agencia_id")
    private Agencia agencia;

    @Column(name = "codigo_seguimiento", length = 50)
    private String codigoSeguimiento;

    @Column(name = "clave_recojo", length = 30)
    private String claveRecojo;

    @Column(name = "resumen_items", nullable = false, columnDefinition = "text")
    private String resumenItems;

    @Column(nullable = false)
    private int bultos;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Version
    private Long version;

    @PrePersist
    void alCrear() {
        creadoEn = Instant.now();
        actualizadoEn = creadoEn;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = Instant.now();
    }
}
