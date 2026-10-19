package com.fitstore.common.storage;

import com.fitstore.common.api.RecursoNoEncontradoException;

/**
 * Puerto de almacenamiento de archivos (ADR-013): una sola interfaz para que el almacenamiento sea portable.
 * Adaptador actual: {@link S3FileStorage} (Garage en local y en la VM; S3 real en AWS).
 */
public interface FileStorage {

    void guardar(TipoBucket bucket, String clave, byte[] contenido, String contentType);

    /** @throws RecursoNoEncontradoException si el archivo no existe */
    byte[] leer(TipoBucket bucket, String clave);

    /** Elimina el archivo; si no existe, no hace nada. */
    void eliminar(TipoBucket bucket, String clave);

    boolean existe(TipoBucket bucket, String clave);

    /** URL con la que el navegador accede a un archivo del bucket público (base {@code MEDIA_BASE_URL}). */
    String urlPublica(String clave);
}
