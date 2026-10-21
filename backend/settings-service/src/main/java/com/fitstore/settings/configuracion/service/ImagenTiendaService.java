package com.fitstore.settings.configuracion.service;

import com.fitstore.settings.configuracion.dto.ImagenRequest;
import com.fitstore.settings.configuracion.dto.ImagenTiendaDto;
import com.fitstore.settings.configuracion.model.TipoImagen;

/** Logos, favicon e imágenes de la landing en el bucket público (CA-3-07, CA-3-14). */
public interface ImagenTiendaService {

    /**
     * Valida el tipo real del archivo y su tamaño, lo guarda y, en los tipos únicos, reemplaza al anterior.
     * Las de landing se redimensionan a 1600 px y se guardan en JPG.
     *
     * @throws com.fitstore.settings.config.SolicitudInvalidaException 400 si el archivo no es válido
     * @throws com.fitstore.common.api.ConflictoNegocioException       409 {@code LIMITE_IMAGENES} si ya hay 5 de landing
     */
    ImagenTiendaDto subir(TipoImagen tipo, byte[] contenido, String textoAlternativo, String enlace);

    ImagenTiendaDto actualizar(Long id, ImagenRequest solicitud);

    void eliminar(Long id);
}
