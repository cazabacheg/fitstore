package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.ImagenProductoRequest;
import com.fitstore.catalog.catalogo.dto.ProductoAdminDto;
import com.fitstore.catalog.catalogo.model.Imagen;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.catalogo.repository.ColorRepository;
import com.fitstore.catalog.catalogo.repository.ImagenRepository;
import com.fitstore.catalog.catalogo.repository.ProductoRepository;
import com.fitstore.catalog.catalogo.repository.VarianteRepository;
import com.fitstore.catalog.config.CatalogoProperties;
import com.fitstore.common.api.FitstoreException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.common.storage.FileStorage;
import com.fitstore.common.storage.TipoBucket;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ImagenServiceImpl implements ImagenService {

    private static final Logger LOG = LoggerFactory.getLogger(ImagenServiceImpl.class);
    static final long MAXIMO_BYTES = 5L * 1024 * 1024;
    private static final float CALIDAD_JPG = 0.85f;
    private static final String MENSAJE_TIPO = "El archivo debe ser una imagen JPG, PNG o WebP";

    private final ProductoRepository productos;
    private final ImagenRepository imagenes;
    private final VarianteRepository variantes;
    private final ColorRepository colores;
    private final ProductoService productoService;
    private final FileStorage almacenamiento;
    private final UrlsImagen urls;
    private final CacheCatalogo cache;
    private final CatalogoProperties propiedades;

    public ImagenServiceImpl(ProductoRepository productos, ImagenRepository imagenes, VarianteRepository variantes,
            ColorRepository colores, ProductoService productoService, FileStorage almacenamiento, UrlsImagen urls,
            CacheCatalogo cache, CatalogoProperties propiedades) {
        this.productos = productos;
        this.imagenes = imagenes;
        this.variantes = variantes;
        this.colores = colores;
        this.productoService = productoService;
        this.almacenamiento = almacenamiento;
        this.urls = urls;
        this.cache = cache;
        this.propiedades = propiedades;
    }

    // ---------------------------------------------------------------- subir

    @Override
    public ProductoAdminDto subir(Long productoId, MultipartFile archivo, Long colorId, boolean principal) {
        Producto producto = productos.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos el producto"));
        if (archivo == null || archivo.isEmpty()) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION", "Elige un archivo de imagen");
        }
        if (archivo.getSize() > MAXIMO_BYTES) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "ARCHIVO_MUY_GRANDE", "El archivo supera los 5 MB permitidos");
        }
        List<Imagen> existentes = imagenes.findByProductoIdOrderByOrdenAsc(productoId);
        if (existentes.size() >= propiedades.imagenes().maxPorProducto()) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "LIMITE_IMAGENES",
                    "Un producto admite hasta " + propiedades.imagenes().maxPorProducto() + " imágenes");
        }
        Long color = resolverColor(productoId, colorId);

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION", "No se pudo leer el archivo");
        }
        String claveBase = "productos/" + productoId + "/" + UUID.randomUUID();
        Dimensiones dimensiones = almacenarVersiones(claveBase, contenido);

        boolean seraPrincipal = principal || existentes.isEmpty();
        if (seraPrincipal) {
            quitarPrincipal(existentes);
        }
        Imagen nueva = new Imagen();
        nueva.setProductoId(productoId);
        nueva.setColorId(color);
        nueva.setClave(claveBase);
        nueva.setOrden(existentes.stream().mapToInt(Imagen::getOrden).max().orElse(-1) + 1);
        nueva.setPrincipal(seraPrincipal);
        nueva.setAncho(dimensiones.ancho());
        nueva.setAlto(dimensiones.alto());
        imagenes.saveAndFlush(nueva);
        cache.invalidarProducto(producto.getSlug());
        return productoService.obtener(productoId);
    }

    /** Con varios colores el color es obligatorio; con uno solo se asigna solo (D3, CA-2-32). */
    private Long resolverColor(Long productoId, Long colorId) {
        if (colorId != null) {
            return colores.findById(colorId).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos el color")).getId();
        }
        Set<Long> delProducto = variantes.findByProductoIdOrderByIdAsc(productoId).stream().map(Variante::getColorId)
                .collect(Collectors.toSet());
        if (delProducto.size() == 1) {
            return delProducto.iterator().next();
        }
        throw new FitstoreException(HttpStatus.BAD_REQUEST, "COLOR_OBLIGATORIO", "Elige el color de la imagen");
    }

    private void quitarPrincipal(List<Imagen> lista) {
        boolean hubo = false;
        for (Imagen i : lista) {
            if (i.isPrincipal()) {
                i.setPrincipal(false);
                hubo = true;
            }
        }
        if (hubo) {
            imagenes.saveAllAndFlush(lista);
        }
    }

    // ---------------------------------------------------------------- editar y eliminar

    @Override
    public ProductoAdminDto editar(Long imagenId, ImagenProductoRequest peticion) {
        Imagen imagen = imagenes.findById(imagenId).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos la imagen"));
        colores.findById(peticion.colorId()).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos el color"));
        List<Imagen> todas = new ArrayList<>(imagenes.findByProductoIdOrderByOrdenAsc(imagen.getProductoId()));
        imagen.setColorId(peticion.colorId());

        if (peticion.principal() && !imagen.isPrincipal()) {
            for (Imagen otra : todas) {
                if (otra.isPrincipal() && !otra.getId().equals(imagenId)) {
                    otra.setPrincipal(false);
                }
            }
            imagenes.saveAllAndFlush(todas);
            imagen.setPrincipal(true);
        }
        // pedir principal = false sobre la principal actual no hace nada: siempre hay exactamente una

        todas.removeIf(i -> i.getId().equals(imagenId));
        todas.add(Math.min(peticion.orden(), todas.size()), imagen);
        for (int i = 0; i < todas.size(); i++) {
            todas.get(i).setOrden(i);
        }
        imagenes.saveAllAndFlush(todas);
        invalidar(imagen.getProductoId());
        return productoService.obtener(imagen.getProductoId());
    }

    @Override
    public ProductoAdminDto eliminar(Long imagenId) {
        Imagen imagen = imagenes.findById(imagenId).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos la imagen"));
        Long productoId = imagen.getProductoId();
        boolean eraPrincipal = imagen.isPrincipal();
        imagenes.delete(imagen);
        imagenes.flush();
        borrarArchivos(imagen.getClave());

        List<Imagen> restantes = imagenes.findByProductoIdOrderByOrdenAsc(productoId);
        for (int i = 0; i < restantes.size(); i++) {
            restantes.get(i).setOrden(i);
        }
        if (eraPrincipal && !restantes.isEmpty()) {
            restantes.get(0).setPrincipal(true);
        }
        imagenes.saveAllAndFlush(restantes);
        invalidar(productoId);
        return productoService.obtener(productoId);
    }

    private void invalidar(Long productoId) {
        productos.findById(productoId).ifPresent(p -> cache.invalidarProducto(p.getSlug()));
    }

    private void borrarArchivos(String clave) {
        try {
            almacenamiento.eliminar(TipoBucket.PUBLICO, urls.claveGrande(clave));
            almacenamiento.eliminar(TipoBucket.PUBLICO, urls.claveMiniatura(clave));
        } catch (RuntimeException e) {
            LOG.warn("No se pudieron borrar los archivos de la imagen {}: {}", clave, e.getMessage());
        }
    }

    // ---------------------------------------------------------------- proceso de imagen

    @Override
    public Dimensiones almacenarVersiones(String claveBase, byte[] contenido) {
        if (!esJpgPngOWebp(contenido)) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "TIPO_NO_PERMITIDO", MENSAJE_TIPO);
        }
        BufferedImage original;
        try {
            // Thumbnailator aplica la orientación EXIF de las fotos del celular
            original = Thumbnails.of(new ByteArrayInputStream(contenido)).scale(1.0).asBufferedImage();
        } catch (IOException | RuntimeException e) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "TIPO_NO_PERMITIDO", MENSAJE_TIPO + " válida (el archivo está dañado)");
        }
        BufferedImage plana = sobreBlanco(original);
        try {
            BufferedImage grande = reducir(plana, propiedades.imagenes().grande());
            BufferedImage miniatura = reducir(plana, propiedades.imagenes().miniatura());
            almacenamiento.guardar(TipoBucket.PUBLICO, urls.claveGrande(claveBase), aJpg(grande), "image/jpeg");
            almacenamiento.guardar(TipoBucket.PUBLICO, urls.claveMiniatura(claveBase), aJpg(miniatura), "image/jpeg");
            return new Dimensiones(grande.getWidth(), grande.getHeight());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar la imagen", e);
        }
    }

    /** Lo que decide es el contenido, no la extensión ni el tipo que declara el navegador (micro 8). */
    static boolean esJpgPngOWebp(byte[] b) {
        if (b == null || b.length < 12) {
            return false;
        }
        boolean jpg = (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
        boolean png = (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G' && b[4] == 0x0D && b[5] == 0x0A
                && b[6] == 0x1A && b[7] == 0x0A;
        boolean webp = b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B'
                && b[11] == 'P';
        return jpg || png || webp;
    }

    /** El JPG no admite transparencia: se dibuja sobre blanco. */
    private static BufferedImage sobreBlanco(BufferedImage origen) {
        BufferedImage rgb = new BufferedImage(origen.getWidth(), origen.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        g.drawImage(origen, 0, 0, null);
        g.dispose();
        return rgb;
    }

    /** Cabe en un cuadrado de {@code lado} px conservando la proporción; una foto más chica no se agranda. */
    private static BufferedImage reducir(BufferedImage origen, int lado) throws IOException {
        if (origen.getWidth() <= lado && origen.getHeight() <= lado) {
            return origen;
        }
        return Thumbnails.of(origen).size(lado, lado).asBufferedImage();
    }

    private static byte[] aJpg(BufferedImage imagen) throws IOException {
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam parametros = escritor.getDefaultWriteParam();
        parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parametros.setCompressionQuality(CALIDAD_JPG);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream flujo = new MemoryCacheImageOutputStream(salida)) {
            escritor.setOutput(flujo);
            escritor.write(null, new IIOImage(imagen, null, null), parametros);
        } finally {
            escritor.dispose();
        }
        return salida.toByteArray();
    }
}
