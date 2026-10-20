package com.fitstore.settings.configuracion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una versión de una política. Cada versión es inmutable; la vigente es la de mayor número. */
@Entity
@Table(name = "politica")
@Getter
@Setter
@NoArgsConstructor
public class Politica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoPolitica tipo;

    @Column(nullable = false)
    private int version;

    /** Markdown. */
    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(name = "publicada_en", nullable = false)
    private Instant publicadaEn;

    @Column(name = "publicada_por", length = 64)
    private String publicadaPor;
}
