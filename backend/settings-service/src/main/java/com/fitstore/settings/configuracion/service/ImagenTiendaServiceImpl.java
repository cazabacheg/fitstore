package com.fitstore.settings.configuracion.service;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.common.storage.FileStorage;
import com.fitstore.common.storage.TipoBucket;
import com.fitstore.settings.config.PropiedadesSettings;
import com.fitstore.settings.config.SolicitudInvalidaException;
import com.fitstore.settings.configuracion.dto.ImagenRequest;
import com.fitstore.settings.configuracion.dto.ImagenTiendaDto;
import com.fitstore.settings.configuracion.model.ConfiguracionTienda;
import com.fitstore.settings.configuracion.model.ImagenTienda;
import com.fitstore.settings.configuracion.model.TipoImagen;
import com.fitstore.settings.configuracion.repository.ConfiguracionTiendaRepository;
import com.fitstore.settings.configuracion.repository.ImagenTiendaRepository;
import com.fitstore.settings.configuracion.service.dominio.FormatoImagen;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImagenTiendaServiceImpl implements ImagenTiendaService {

    private static final String CAMPO = "archivo";
    private static final double CALIDAD_JPG = 0.85;

    private final ImagenTiendaRepository imagenes;
    private final ConfiguracionTiendaRepository configuracion;
    private final FileStorage almacen;
    private final PublicadorConfiguracion publicador;
    private final PropiedadesSettings propiedades;
    private final Clock reloj;

    public ImagenTiendaServiceImpl(ImagenTiendaRepository imagenes, ConfiguracionTiendaRepository configuracion,
            FileStorage almacen, PublicadorConfiguracion publicador, PropiedadesSettings propiedades, Clock reloj) {
        this.imagenes = imagenes;
        this.configuracion = configuracion;
        this.almacen = almacen;
        this.publicador = publicador;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public ImagenTiendaDto subir(TipoImagen tipo, byte[] contenido, String textoAlternativo, String enlace) {
        boolean esLanding = tipo == TipoImagen.LANDING;
        validarTamano(contenido, esLanding);
        FormatoImagen formato = FormatoImagen.detectar(contenido)
                .filter(f -> formatosPermitidos(tipo).contains(f))
                .orElseThrow(() -> new SolicitudInvalidaException(CAMPO, mensajeDeFormatos(tipo)));
        if (esLanding && imagenes.countByTipo(TipoImagen.LANDING) >= propiedades.imagenes().landingMaxCantidad()) {
            throw new ConflictoNegocioException("LIMITE_IMAGENES",
                    "La landing admite hasta %d imágenes; elimina una para subir otra"
                            .formatted(propiedades.imagenes().landingMaxCantidad()));
        }

        byte[] aGuardar = contenido;
        FormatoImagen formatoFinal = formato;
        if (esLanding) {
            aGuardar = redimensionarAJpg(contenido);
            formatoFinal = FormatoImagen.JPEG;
        }
        String clave = "tienda/%s/%s.%s".formatted(tipo.name().toLowerCase(), UUID.randomUUID(), formatoFinal.extension());
        almacen.guardar(TipoBucket.PUBLICO, clave, aGuardar, formatoFinal.contentType());

        List<ImagenTienda> anteriores = esLanding ? List.of() : imagenes.findByTipoOrderByOrdenAsc(tipo);
        if (!anteriores.isEmpty()) {
            imagenes.deleteAll(anteriores);
            imagenes.flush();
        }
        ImagenTienda nueva = new ImagenTienda();
        nueva.setTipo(tipo);
        nueva.setClave(clave);
        nueva.setOrden(esLanding ? siguienteOrden() : 0);
        nueva.setTextoAlternativo(textoAlternativo);
        nueva.setEnlace(enlace);
        nueva.setCreadoEn(Instant.now(reloj));
        nueva = imagenes.saveAndFlush(nueva);
        anteriores.forEach(a -> almacen.eliminar(TipoBucket.PUBLICO, a.getClave()));

        publicar();
        return dto(nueva);
    }

    @Override
    @Transactional
    public ImagenTiendaDto actualizar(Long id, ImagenRequest solicitud) {
        ImagenTienda imagen = buscar(id);
        imagen.setOrden(solicitud.orden());
        imagen.setTextoAlternativo(solicitud.textoAlternativo());
        imagen.setEnlace(solicitud.enlace());
        imagen = imagenes.saveAndFlush(imagen);
        publicar();
        return dto(imagen);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        ImagenTienda imagen = buscar(id);
        imagenes.delete(imagen);
        imagenes.flush();
        almacen.eliminar(TipoBucket.PUBLICO, imagen.getClave());
        publicar();
    }

    private ImagenTienda buscar(Long id) {
        return imagenes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos la imagen"));
    }

    private void publicar() {
        ConfiguracionTienda actual = configuracion.findById(1)
                .orElseThrow(() -> new RecursoNoEncontradoException("La tienda aún no tiene configuración"));
        publicador.publicar(actual, List.of("IMAGENES"));
    }

    private ImagenTiendaDto dto(ImagenTienda i) {
        return new ImagenTiendaDto(i.getId(), i.getTipo(), almacen.urlPublica(i.getClave()), i.getOrden(),
                i.getTextoAlternativo(), i.getEnlace());
    }

    private int siguienteOrden() {
        return imagenes.findByTipoOrderByOrdenAsc(TipoImagen.LANDING).stream().mapToInt(ImagenTienda::getOrden).max()
                .orElse(-1) + 1;
    }

    private void validarTamano(byte[] contenido, boolean esLanding) {
        if (contenido.length == 0) {
            throw new SolicitudInvalidaException(CAMPO, "Elige un archivo");
        }
        long maximo = (esLanding ? propiedades.imagenes().landingMax() : propiedades.imagenes().logoMax()).toBytes();
        if (contenido.length > maximo) {
            throw new SolicitudInvalidaException(CAMPO, "El archivo pesa más de %d MB".formatted(maximo / (1024 * 1024)));
        }
    }

    private static Set<FormatoImagen> formatosPermitidos(TipoImagen tipo) {
        return switch (tipo) {
            case LOGO_CLARO, LOGO_OSCURO -> EnumSet.of(FormatoImagen.PNG, FormatoImagen.WEBP);
            case FAVICON -> EnumSet.of(FormatoImagen.PNG, FormatoImagen.WEBP, FormatoImagen.ICO);
            case LANDING -> EnumSet.of(FormatoImagen.JPEG, FormatoImagen.WEBP);
        };
    }

    private static String mensajeDeFormatos(TipoImagen tipo) {
        return switch (tipo) {
            case LOGO_CLARO, LOGO_OSCURO -> "El logo debe ser PNG o WebP";
            case FAVICON -> "El favicon debe ser PNG, WebP o ICO";
            case LANDING -> "La imagen de la landing debe ser JPG o WebP";
        };
    }

    /** Reduce a {@code landing-ancho} px (sin agrandar las más chicas) y guarda en JPG sobre fondo blanco. */
    private byte[] redimensionarAJpg(byte[] contenido) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(contenido));
            if (original == null) {
                throw new SolicitudInvalidaException(CAMPO, "No pudimos leer la imagen");
            }
            BufferedImage sinTransparencia = new BufferedImage(original.getWidth(), original.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = sinTransparencia.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, original.getWidth(), original.getHeight());
            g.drawImage(original, 0, 0, null);
            g.dispose();

            int ancho = propiedades.imagenes().landingAncho();
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            var constructor = Thumbnails.of(sinTransparencia);
            if (sinTransparencia.getWidth() > ancho) {
                constructor.width(ancho);
            } else {
                constructor.scale(1.0);
            }
            constructor.outputFormat("jpg").outputQuality(CALIDAD_JPG).toOutputStream(salida);
            return salida.toByteArray();
        } catch (IOException | RuntimeException e) {
            if (e instanceof SolicitudInvalidaException invalida) {
                throw invalida;
            }
            throw new SolicitudInvalidaException(CAMPO, "No pudimos procesar la imagen");
        }
    }
}
