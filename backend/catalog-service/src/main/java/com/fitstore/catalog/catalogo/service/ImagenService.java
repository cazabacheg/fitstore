package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.ImagenProductoRequest;
import com.fitstore.catalog.catalogo.dto.ProductoAdminDto;
import org.springframework.web.multipart.MultipartFile;

/** Imágenes de producto: tipo real, redimensionado a 1200 y 400 px en JPG, color y principal (HU-2-06, micro 4.4). */
public interface ImagenService {

    /** Ancho y alto de la versión grande que quedó guardada. */
    record Dimensiones(int ancho, int alto) {
    }

    /**
     * Sube una imagen al producto. El color es obligatorio si el producto tiene varios (se asigna solo si tiene uno);
     * la primera imagen queda como principal.
     */
    ProductoAdminDto subir(Long productoId, MultipartFile archivo, Long colorId, boolean principal);

    /** Cambia orden, color o principal; siempre queda exactamente una principal. */
    ProductoAdminDto editar(Long imagenId, ImagenProductoRequest peticion);

    /** Borra la imagen y sus archivos; si era la principal, pasa a serlo la siguiente en orden. */
    ProductoAdminDto eliminar(Long imagenId);

    /**
     * Valida el tipo real del contenido, genera las dos versiones JPG y las guarda en el bucket público como
     * {@code {claveBase}-1200.jpg} y {@code {claveBase}-400.jpg}. Es el mismo proceso de una subida del admin y lo usa la
     * carga de las fotos de demostración.
     */
    Dimensiones almacenarVersiones(String claveBase, byte[] contenido);
}
