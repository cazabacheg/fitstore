package com.fitstore.settings.configuracion.dto;

/** Configuración pública de la tienda: nunca incluye correos para alertas, autores, IPs ni secretos. */
public record ConfiguracionPublicaDto(IdentidadDto identidad, TemaDto tema, ContenidoDto contenido,
        ParametrosPublicosDto parametros, VersionesPoliticasDto politicas) {
}
