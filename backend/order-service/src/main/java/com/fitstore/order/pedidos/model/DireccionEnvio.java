package com.fitstore.order.pedidos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dirección de entrega con los nombres de departamento, provincia y distrito congelados (D3). No existe en recojo. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DireccionEnvio {

    @Column(name = "dir_ubigeo", length = 6)
    private String ubigeo;

    @Column(name = "dir_departamento", length = 60)
    private String departamento;

    @Column(name = "dir_provincia", length = 60)
    private String provincia;

    @Column(name = "dir_distrito", length = 60)
    private String distrito;

    @Column(name = "dir_direccion", length = 255)
    private String direccion;

    @Column(name = "dir_referencia", length = 255)
    private String referencia;
}
