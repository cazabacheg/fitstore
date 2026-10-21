package com.fitstore.settings.configuracion.service.dominio;

import java.util.Optional;

/**
 * Formatos de imagen que acepta la tienda, reconocidos por los primeros bytes del archivo y no por su extensión ni
 * por el tipo que declara el navegador. SVG no existe aquí a propósito: puede llevar scripts.
 */
public enum FormatoImagen {
    PNG("png", "image/png"),
    JPEG("jpg", "image/jpeg"),
    WEBP("webp", "image/webp"),
    ICO("ico", "image/x-icon");

    private final String extension;
    private final String contentType;

    FormatoImagen(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    /** @return el formato real de los bytes, o vacío si no es PNG, JPG, WebP ni ICO */
    public static Optional<FormatoImagen> detectar(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return Optional.of(PNG);
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return Optional.of(JPEG);
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return Optional.of(WEBP);
        }
        if (bytes.length >= 4 && bytes[0] == 0 && bytes[1] == 0 && bytes[2] == 1 && bytes[3] == 0) {
            return Optional.of(ICO);
        }
        return Optional.empty();
    }
}
