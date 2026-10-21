package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.ImagenProductoDto;
import com.fitstore.catalog.catalogo.model.Imagen;
import com.fitstore.catalog.config.CatalogoProperties;
import com.fitstore.common.storage.FileStorage;
import org.springframework.stereotype.Component;

/**
 * Claves y URLs de las dos versiones de cada imagen (micro 3.3): la columna {@code imagen.clave} guarda la clave base
 * ({@code productos/{productoId}/{uuid}}) y los archivos son {@code {clave}-1200.jpg} y {@code {clave}-400.jpg}.
 */
@Component
public class UrlsImagen {

    private final FileStorage almacenamiento;
    private final CatalogoProperties propiedades;

    public UrlsImagen(FileStorage almacenamiento, CatalogoProperties propiedades) {
        this.almacenamiento = almacenamiento;
        this.propiedades = propiedades;
    }

    public String claveGrande(String clave) {
        return clave + "-" + propiedades.imagenes().grande() + ".jpg";
    }

    public String claveMiniatura(String clave) {
        return clave + "-" + propiedades.imagenes().miniatura() + ".jpg";
    }

    public String urlGrande(String clave) {
        return almacenamiento.urlPublica(claveGrande(clave));
    }

    public String urlMiniatura(String clave) {
        return almacenamiento.urlPublica(claveMiniatura(clave));
    }

    public ImagenProductoDto aDto(Imagen imagen) {
        return new ImagenProductoDto(imagen.getId(), imagen.getColorId(), urlGrande(imagen.getClave()),
                urlMiniatura(imagen.getClave()), imagen.getOrden(), imagen.isPrincipal(), imagen.getAncho(), imagen.getAlto());
    }
}
