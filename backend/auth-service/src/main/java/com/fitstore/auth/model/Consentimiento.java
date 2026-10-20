package com.fitstore.auth.model;

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
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Prueba del consentimiento (Ley 29733, D5): versión de la política, fecha e IP. */
@Entity
@Table(name = "consentimiento")
@Getter
@Setter
public class Consentimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoConsentimiento tipo;

    @Column(name = "version_politica", nullable = false, length = 20)
    private String versionPolitica;

    @Column(name = "aceptado_en", nullable = false)
    private Instant aceptadoEn;

    @Column(nullable = false, length = 45)
    private String ip;
}
