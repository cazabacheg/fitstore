package com.fitstore.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Textos editables de cada correo (micro 2.1 D2 y 3.1). El diseño vive en el código, no aquí. */
@Entity
@Table(name = "plantilla")
@Getter
@Setter
@NoArgsConstructor
public class Plantilla {

    @Id
    @Column(length = 40)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "text")
    private String mensaje;

    @Column(name = "texto_boton", length = 60)
    private String textoBoton;

    @Column(nullable = false)
    private boolean activa;

    @Column(nullable = false)
    private boolean obligatoria;

    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    @Column(name = "actualizada_por", length = 100)
    private String actualizadaPor;
}
