package com.fitstore.settings.configuracion.dto;

/** Versión vigente de cada política (la usa el registro de la spec 1). */
public record VersionesPoliticasDto(int terminos, int privacidad, int cambiosDevoluciones, int envios) {
}
