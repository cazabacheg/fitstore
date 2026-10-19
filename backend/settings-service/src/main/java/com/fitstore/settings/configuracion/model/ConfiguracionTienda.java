package com.fitstore.settings.configuracion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configuración de la tienda: una sola fila (id = 1). La versión evita que dos administradores se pisen. */
@Entity
@Table(name = "configuracion_tienda")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionTienda {

    @Id
    private Integer id;

    @Embedded
    private Identidad identidad = new Identidad();

    @Embedded
    private Tema tema = new Tema();

    @Embedded
    private Contenido contenido = new Contenido();

    @Embedded
    private Parametros parametros = new Parametros();

    @Version
    @Column(nullable = false)
    private Integer version;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Column(name = "actualizado_por", length = 64)
    private String actualizadoPor;
}
