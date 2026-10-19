package com.fitstore.settings.configuracion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Los cinco colores del tema (formato #RRGGBB); la tipografía y los bordes son fijos del sistema de diseño. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Tema {

    @Column(name = "color_primario", nullable = false, length = 7)
    private String colorPrimario;

    @Column(name = "color_secundario", nullable = false, length = 7)
    private String colorSecundario;

    @Column(name = "color_acento", nullable = false, length = 7)
    private String colorAcento;

    @Column(name = "color_fondo", nullable = false, length = 7)
    private String colorFondo;

    @Column(name = "color_texto", nullable = false, length = 7)
    private String colorTexto;
}
