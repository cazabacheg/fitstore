package com.fitstore.settings.reclamaciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Producto o servicio materia del reclamo. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class BienContratado {

    @Enumerated(EnumType.STRING)
    @Column(name = "bien_tipo", nullable = false, length = 10)
    private TipoBien tipo;

    @Column(name = "bien_descripcion", nullable = false, length = 300)
    private String descripcion;

    @Column(name = "monto_reclamado", precision = 12, scale = 2)
    private BigDecimal montoReclamado;

    @Column(name = "pedido_codigo", length = 20)
    private String pedidoCodigo;
}
