package com.fitstore.settings.configuracion.service;

import com.fitstore.common.storage.FileStorage;
import com.fitstore.common.storage.TipoBucket;
import com.fitstore.settings.configuracion.model.ImagenTienda;
import com.fitstore.settings.configuracion.model.TipoImagen;
import com.fitstore.settings.configuracion.repository.ImagenTiendaRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Con los perfiles {@code dev} y {@code demo}: sube a Garage las 3 imágenes de landing de licencia libre
 * ({@code resources/demo/imagenes}, ver CREDITOS.md) si el bucket no las tiene, y las registra si la tienda aún
 * no tiene ninguna imagen de landing. Mismo mecanismo que catalog (SPEC-2).
 */
@Component
@Profile({"dev", "demo"})
public class DemoImagenesInicializador implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoImagenesInicializador.class);

    /** Archivo, texto alternativo y enlace de cada imagen, en el orden del carrusel. */
    private static final List<Demo> IMAGENES = List.of(
            new Demo("landing-1.jpg", "Mujer entrenando con top y leggings deportivos", "/productos"),
            new Demo("landing-2.jpg", "Ropa deportiva cómoda para entrenar todos los días", "/productos"),
            new Demo("landing-3.jpg", "Conjunto deportivo de compresión hecho en Gamarra", "/productos"));

    private final ImagenTiendaRepository imagenes;
    private final FileStorage almacen;
    private final CachePublica cache;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    public DemoImagenesInicializador(ImagenTiendaRepository imagenes, FileStorage almacen, CachePublica cache,
            TransactionTemplate transaccion, Clock reloj) {
        this.imagenes = imagenes;
        this.almacen = almacen;
        this.cache = cache;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        boolean sinLanding = imagenes.countByTipo(TipoImagen.LANDING) == 0;
        for (int i = 0; i < IMAGENES.size(); i++) {
            Demo demo = IMAGENES.get(i);
            String clave = "tienda/landing/demo-%d.jpg".formatted(i + 1);
            int orden = i;
            if (sinLanding && !imagenes.existsByClave(clave)) {
                transaccion.executeWithoutResult(e -> imagenes.save(nueva(clave, orden, demo)));
            }
            if (imagenes.existsByClave(clave) && !almacen.existe(TipoBucket.PUBLICO, clave)) {
                almacen.guardar(TipoBucket.PUBLICO, clave, leer(demo.archivo()), "image/jpeg");
                LOG.info("Imagen de demostración {} subida al bucket público", clave);
            }
        }
        cache.invalidar();
    }

    private ImagenTienda nueva(String clave, int orden, Demo demo) {
        ImagenTienda imagen = new ImagenTienda();
        imagen.setTipo(TipoImagen.LANDING);
        imagen.setClave(clave);
        imagen.setOrden(orden);
        imagen.setTextoAlternativo(demo.textoAlternativo());
        imagen.setEnlace(demo.enlace());
        imagen.setCreadoEn(Instant.now(reloj));
        return imagen;
    }

    private static byte[] leer(String archivo) {
        try {
            return new ClassPathResource("demo/imagenes/" + archivo).getContentAsByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la imagen de demostración " + archivo, e);
        }
    }

    private record Demo(String archivo, String textoAlternativo, String enlace) {
    }
}
