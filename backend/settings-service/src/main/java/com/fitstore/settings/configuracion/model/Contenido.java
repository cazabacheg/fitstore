package com.fitstore.settings.configuracion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Contenido de la landing, contacto, redes y tienda física. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Contenido {

    @Column(name = "hero_titulo", nullable = false, length = 80)
    private String heroTitulo;

    @Column(name = "hero_subtitulo", length = 160)
    private String heroSubtitulo;

    @Column(name = "hero_cta_texto", length = 30)
    private String heroCtaTexto;

    @Column(name = "hero_cta_enlace", length = 300)
    private String heroCtaEnlace;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<Beneficio> beneficios = new ArrayList<>();

    @Column(length = 150)
    private String email;

    @Column(length = 15)
    private String telefono;

    @Column(length = 9)
    private String whatsapp;

    @Column(name = "instagram_url", length = 300)
    private String instagramUrl;

    @Column(name = "facebook_url", length = 300)
    private String facebookUrl;

    @Column(name = "tiktok_url", length = 300)
    private String tiktokUrl;

    @Column(length = 120)
    private String horario;

    @Column(name = "tienda_direccion", length = 200)
    private String tiendaDireccion;

    @Column(name = "tienda_ubigeo", length = 6)
    private String tiendaUbigeo;

    @Column(name = "tienda_referencia", length = 200)
    private String tiendaReferencia;
}
