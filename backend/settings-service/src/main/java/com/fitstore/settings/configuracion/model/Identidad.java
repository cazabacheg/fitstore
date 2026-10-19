package com.fitstore.settings.configuracion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Identidad y datos legales de la tienda. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Identidad {

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(length = 120)
    private String eslogan;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(nullable = false, length = 11)
    private String ruc;

    @Column(name = "direccion_fiscal", nullable = false, length = 200)
    private String direccionFiscal;
}
