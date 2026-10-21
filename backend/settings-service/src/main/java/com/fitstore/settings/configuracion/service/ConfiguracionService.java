package com.fitstore.settings.configuracion.service;

import com.fitstore.settings.configuracion.dto.ConfiguracionAdminDto;
import com.fitstore.settings.configuracion.dto.ConfiguracionPublicaDto;
import com.fitstore.settings.configuracion.dto.ContenidoRequest;
import com.fitstore.settings.configuracion.dto.IdentidadRequest;
import com.fitstore.settings.configuracion.dto.ParametrosRequest;
import com.fitstore.settings.configuracion.dto.TemaRequest;

/** Configuración de la tienda por secciones, con versión optimista (CU-31, CU-32). */
public interface ConfiguracionService {

    /** Configuración pública: desde Redis si está, si no desde la base (CA-3-35, CA-3-37). */
    ConfiguracionPublicaDto obtenerPublica();

    ConfiguracionAdminDto obtenerAdmin();

    /** @throws com.fitstore.common.api.ConflictoNegocioException {@code CONFLICTO_VERSION} si otro admin guardó antes */
    ConfiguracionAdminDto guardarIdentidad(IdentidadRequest solicitud, String actor);

    /** Guarda aunque el contraste sea bajo: las advertencias viajan en la respuesta (RNF-19). */
    ConfiguracionAdminDto guardarTema(TemaRequest solicitud, String actor);

    ConfiguracionAdminDto guardarContenido(ContenidoRequest solicitud, String actor);

    ConfiguracionAdminDto guardarParametros(ParametrosRequest solicitud, String actor);
}
