package com.fitstore.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Reembolso de un cargo aprobado, total o parcial. La suma de los reembolsos nunca supera lo cobrado (invariante 4). */
@Entity
@Table(name = "reembolso")
@Getter
@Setter
public class Reembolso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "intencion_id", nullable = false)
    private UUID intencionId;

    @Column(name = "transaccion_id", nullable = false)
    private UUID transaccionId;

    /** Id de la solicitud de order; solo en reembolsos postventa. */
    @Column(name = "solicitud_id", unique = true)
    private UUID solicitudId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MotivoReembolso motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReembolso estado;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "proximo_intento_en")
    private Instant proximoIntentoEn;

    @Column(name = "id_proveedor", length = 100)
    private String idProveedor;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    @Column(name = "resuelto_por", length = 100)
    private String resueltoPor;

    @Column(length = 500)
    private String nota;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        if (creadoEn == null) {
            creadoEn = ahora;
        }
        actualizadoEn = ahora;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = Instant.now();
    }
}
