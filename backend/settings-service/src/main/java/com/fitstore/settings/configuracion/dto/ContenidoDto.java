package com.fitstore.settings.configuracion.dto;

import java.util.List;

/** Contenido de la landing, contacto, redes y tienda física, con las imágenes de la landing en su orden. */
public record ContenidoDto(String heroTitulo, String heroSubtitulo, String heroCtaTexto, String heroCtaEnlace,
        List<BeneficioDto> beneficios, String email, String telefono, String whatsapp, String instagramUrl,
        String facebookUrl, String tiktokUrl, String horario, TiendaFisicaDto tiendaFisica,
        List<ImagenTiendaDto> imagenesLanding) {
}
