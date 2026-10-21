package com.fitstore.catalog.catalogo.demo;

import com.fitstore.catalog.catalogo.service.ImagenService;
import com.fitstore.catalog.catalogo.service.UrlsImagen;
import com.fitstore.catalog.config.CatalogoProperties;
import com.fitstore.common.storage.FileStorage;
import com.fitstore.common.storage.TipoBucket;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Al arrancar con el perfil {@code dev} o {@code demo}, sube a Garage las fotos de demostración que el bucket aún no tiene
 * (SPEC-2 sección 6). Usa el mismo proceso que una subida del admin ({@link ImagenService#almacenarVersiones}: versiones de
 * 1200 y 400 px en JPG) y las claves de las filas de {@code imagen} que sembró {@code R__catalogo_demo.sql}
 * ({@code productos/{id}/demo-{color}}, ids 20001 a 29999). Si Garage no responde solo lo registra: el servicio arranca igual.
 */
@Component
@Profile({"dev", "demo"})
public class CargadorImagenesDemo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CargadorImagenesDemo.class);
    private static final String PREFIJO_CLAVE = "demo-";

    private final JdbcClient jdbc;
    private final FileStorage almacenamiento;
    private final UrlsImagen urls;
    private final ImagenService imagenes;
    private final CatalogoProperties propiedades;

    public CargadorImagenesDemo(JdbcClient jdbc, FileStorage almacenamiento, UrlsImagen urls, ImagenService imagenes,
            CatalogoProperties propiedades) {
        this.jdbc = jdbc;
        this.almacenamiento = almacenamiento;
        this.urls = urls;
        this.imagenes = imagenes;
        this.propiedades = propiedades;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        subirLasQueFaltan();
    }

    /** @return cuántas fotos subió (las que el bucket ya tenía no se tocan) */
    public int subirLasQueFaltan() {
        if (!propiedades.demo().subirImagenes()) {
            return 0;
        }
        List<Map<String, Object>> filas = jdbc.sql("SELECT producto_id, clave FROM imagen WHERE id BETWEEN 20001 AND 29999 ORDER BY id")
                .query().listOfRows();
        int subidas = 0;
        for (Map<String, Object> fila : filas) {
            String clave = (String) fila.get("clave");
            try {
                if (almacenamiento.existe(TipoBucket.PUBLICO, urls.claveGrande(clave))
                        && almacenamiento.existe(TipoBucket.PUBLICO, urls.claveMiniatura(clave))) {
                    continue;
                }
                byte[] foto = leerFoto(((Number) fila.get("producto_id")).longValue(), clave);
                if (foto == null) {
                    LOG.warn("No hay foto de demostración para {}", clave);
                    continue;
                }
                imagenes.almacenarVersiones(clave, foto);
                subidas++;
            } catch (RuntimeException e) {
                LOG.warn("No se pudo subir la foto de demostración {}: {}", clave, e.getMessage());
            }
        }
        LOG.info("Fotos de demostración: {} subidas a Garage de {} registradas", subidas, filas.size());
        return subidas;
    }

    /** {@code productos/1001/demo-azul-marino} → {@code demo/imagenes/p1001-azul-marino.jpg} */
    private static byte[] leerFoto(long productoId, String clave) {
        String color = clave.substring(clave.lastIndexOf('/') + 1);
        if (!color.startsWith(PREFIJO_CLAVE)) {
            return null;
        }
        ClassPathResource recurso = new ClassPathResource("demo/imagenes/p" + productoId + "-" + color.substring(PREFIJO_CLAVE.length()) + ".jpg");
        if (!recurso.exists()) {
            return null;
        }
        try (InputStream entrada = recurso.getInputStream()) {
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + recurso.getPath(), e);
        }
    }
}
