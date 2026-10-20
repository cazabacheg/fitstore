package com.fitstore.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Copia local de la configuración pública de la tienda (micro 2.1 D3): una sola fila ({@code id = 1}) que se
 * actualiza con cada {@code ConfiguracionActualizada}. {@code version} es la de settings, no un control de concurrencia.
 */
@Entity
@Table(name = "marca_tienda")
@Getter
@Setter
@NoArgsConstructor
public class MarcaTienda {

    @Id
    private Short id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> configuracion;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "correos_alertas", nullable = false, columnDefinition = "text[]")
    private String[] correosAlertas;

    @Column(nullable = false)
    private int version;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;
}
