package com.fitstore.order.pedidos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Agencia, plazo estimado y seguimiento del envío; {@code agenciaId} solo en la modalidad agencia. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Envio {

    @Column(name = "agencia_id")
    private Long agenciaId;

    @Column(name = "agencia_nombre", length = 120)
    private String agenciaNombre;

    @Column(name = "plazo_dias_min")
    private Integer plazoDiasMin;

    @Column(name = "plazo_dias_max")
    private Integer plazoDiasMax;

    @Column(name = "codigo_seguimiento", length = 60)
    private String codigoSeguimiento;
}
