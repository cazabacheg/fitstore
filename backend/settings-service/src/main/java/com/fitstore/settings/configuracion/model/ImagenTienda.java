package com.fitstore.settings.configuracion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Imagen de la tienda guardada en Garage (la clave es {@code tienda/{tipo}/{uuid}.{ext}}). */
@Entity
@Table(name = "imagen_tienda")
@Getter
@Setter
@NoArgsConstructor
public class ImagenTienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoImagen tipo;

    @Column(nullable = false, length = 200)
    private String clave;

    @Column(nullable = false)
    private int orden;

    @Column(name = "texto_alternativo", length = 200)
    private String textoAlternativo;

    @Column(length = 300)
    private String enlace;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;
}
