package com.fitstore.settings.reclamaciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Reclamo o queja del Libro de Reclamaciones. Es un registro legal: nunca se elimina (D8). */
@Entity
@Table(name = "reclamo")
@Getter
@Setter
@NoArgsConstructor
public class Reclamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** {@code RC-2026-000001}. */
    @Column(nullable = false, length = 20)
    private String numero;

    @Column(nullable = false)
    private int anio;

    @Column(nullable = false)
    private int correlativo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoReclamo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReclamo estado;

    @Embedded
    private Consumidor consumidor = new Consumidor();

    @Embedded
    private BienContratado bien = new BienContratado();

    @Column(nullable = false, columnDefinition = "text")
    private String detalle;

    @Column(name = "pedido_consumidor", nullable = false, columnDefinition = "text")
    private String pedidoConsumidor;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDate fechaLimite;

    @Column(columnDefinition = "text")
    private String respuesta;

    @Column(name = "respondido_en")
    private Instant respondidoEn;

    @Column(name = "respondido_por", length = 64)
    private String respondidoPor;

    @Column(length = 45)
    private String ip;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;
}
