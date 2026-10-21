package com.fitstore.settings.configuracion.service;

import com.fitstore.settings.configuracion.dto.PoliticaDto;
import com.fitstore.settings.configuracion.dto.VersionPoliticaDto;
import java.util.List;

/** Políticas versionadas: cada versión es inmutable y la vigente es la de mayor número (D2). */
public interface PoliticaService {

    /**
     * @param tipo    terminos, privacidad, cambios-devoluciones o envios
     * @param version {@code null} para la vigente
     * @throws com.fitstore.common.api.RecursoNoEncontradoException si el tipo o la versión no existen
     */
    PoliticaDto obtener(String tipo, Integer version);

    /** Historial de versiones, la más reciente primero y sin el texto. */
    List<VersionPoliticaDto> versiones(String tipo);

    /** Crea la versión siguiente y pasa a ser la vigente; invalida la caché de {@code /settings/public}. */
    PoliticaDto publicar(String tipo, String contenido, String autor);
}
