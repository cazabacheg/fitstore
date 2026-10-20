package com.fitstore.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Copia local del resumen de cada pedido, tomada de {@code PedidoCreado} (micro 2.1 D4). Sin DNI. */
@Entity
@Table(name = "pedido_resumen")
@Getter
@Setter
@NoArgsConstructor
public class PedidoResumen {

    @Id
    @Column(name = "pedido_id")
    private UUID pedidoId;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 200)
    private String nombre;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> items;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> totales;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Modalidad modalidad;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;
}
