package com.fitstore.settings.reclamaciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos del consumidor de la Hoja de Reclamación (el apoderado es obligatorio si es menor de edad). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Consumidor {

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 80)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 3)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 12)
    private String numeroDocumento;

    @Column(nullable = false, length = 200)
    private String domicilio;

    @Column(nullable = false, length = 15)
    private String telefono;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "es_menor", nullable = false)
    private boolean esMenor;

    @Column(name = "apoderado_nombre", length = 160)
    private String apoderadoNombre;
}
