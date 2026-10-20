package com.fitstore.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Boleta simulada de una intención pagada (una por intención). El PDF vive en Garage, bucket privado (D6). */
@Entity
@Table(name = "boleta")
@Getter
@Setter
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "intencion_id", nullable = false, unique = true)
    private UUID intencionId;

    @Column(nullable = false, length = 10)
    private String serie;

    @Column(nullable = false)
    private int correlativo;

    /** Número legible: {@code B001-00000001}. */
    @Column(nullable = false, length = 20)
    private String numero;

    /** Ruta del PDF en Garage: {@code boletas/{anio}/{numero}.pdf}. */
    @Column(nullable = false)
    private String clave;

    @Column(name = "emitida_en", nullable = false)
    private Instant emitidaEn;
}
