package com.fitstore.settings.configuracion.dto;

import java.time.Instant;
import java.util.List;

/** Configuración completa para el backoffice: lo público más lo interno y las advertencias no bloqueantes. */
public record ConfiguracionAdminDto(IdentidadDto identidad, TemaDto tema, ContenidoDto contenido,
        ParametrosPublicosDto parametros, VersionesPoliticasDto politicas, List<String> correosAlertas, int version,
        Instant actualizadoEn, String actualizadoPor, List<String> advertencias) {
}
