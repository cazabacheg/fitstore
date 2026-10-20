package com.fitstore.delivery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Copia de la dirección de envío que llega en {@code PedidoListoDespacho} (D3). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DireccionEnvio {

    @Column(name = "dir_ubigeo", nullable = false, length = 6)
    private String ubigeo;

    @Column(name = "dir_departamento", nullable = false, length = 60)
    private String departamento;

    @Column(name = "dir_provincia", nullable = false, length = 60)
    private String provincia;

    @Column(name = "dir_distrito", nullable = false, length = 60)
    private String distrito;

    @Column(name = "dir_direccion", nullable = false, length = 200)
    private String direccion;

    @Column(name = "dir_referencia", length = 200)
    private String referencia;
}
