package com.fitstore.settings.configuracion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Parámetros de negocio: pagos, entregas, expiración del pedido, reembolso postventa y correos de alertas. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Parametros {

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Enumerated(EnumType.STRING)
    @Column(name = "metodos_pago", nullable = false, columnDefinition = "text[]")
    private Set<MetodoPago> metodosPago = new HashSet<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Enumerated(EnumType.STRING)
    @Column(name = "modalidades_entrega", nullable = false, columnDefinition = "text[]")
    private Set<ModalidadEntrega> modalidadesEntrega = new HashSet<>();

    @Column(name = "expiracion_pedido_minutos", nullable = false)
    private int expiracionPedidoMinutos = 30;

    /** 0 = sin límite. */
    @Column(name = "dias_reembolso_postventa", nullable = false)
    private int diasReembolsoPostventa = 30;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "correos_alertas", nullable = false, columnDefinition = "text[]")
    private List<String> correosAlertas = new ArrayList<>();
}
