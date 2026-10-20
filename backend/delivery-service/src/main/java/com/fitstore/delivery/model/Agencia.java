package com.fitstore.delivery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Agencia de transporte (dato mock editable). {@code urlSeguimiento} es una plantilla con {@code {codigo}} (D4). */
@Entity
@Table(name = "agencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "url_seguimiento", length = 300)
    private String urlSeguimiento;

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;

    /** Posición en el select del checkout. */
    @Column(nullable = false)
    private int orden;
}
