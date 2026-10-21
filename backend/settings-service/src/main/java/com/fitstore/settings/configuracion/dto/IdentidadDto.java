package com.fitstore.settings.configuracion.dto;

/** Identidad y datos legales; las URLs de logos y favicon son públicas (bucket de medios). */
public record IdentidadDto(String nombre, String eslogan, String razonSocial, String ruc, String direccionFiscal,
        String logoClaroUrl, String logoOscuroUrl, String faviconUrl) {
}
