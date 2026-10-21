package com.fitstore.settings.configuracion.dto;

import com.fitstore.settings.configuracion.model.TipoImagen;

/** Imagen de la tienda con su URL pública. */
public record ImagenTiendaDto(Long id, TipoImagen tipo, String url, int orden, String textoAlternativo,
        String enlace) {
}
