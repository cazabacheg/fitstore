package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.DescuentoProductoRequest;
import com.fitstore.catalog.catalogo.dto.DestacadoRequest;
import com.fitstore.catalog.catalogo.dto.EditarVarianteRequest;
import com.fitstore.catalog.catalogo.dto.ImagenProductoDto;
import com.fitstore.catalog.catalogo.dto.MatrizRequest;
import com.fitstore.catalog.catalogo.dto.ProductoAdminDto;
import com.fitstore.catalog.catalogo.dto.ProductoAdminResumenDto;
import com.fitstore.catalog.catalogo.dto.ProductoRequest;
import com.fitstore.catalog.catalogo.dto.TramoDto;
import com.fitstore.catalog.catalogo.dto.VarianteAdminDto;
import com.fitstore.catalog.catalogo.dto.VarianteRequest;
import com.fitstore.catalog.catalogo.mapper.CatalogoMapper;
import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.Color;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.model.Imagen;
import com.fitstore.catalog.catalogo.model.ModoDescuento;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Talla;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.catalogo.repository.CategoriaRepository;
import com.fitstore.catalog.catalogo.repository.ColorRepository;
import com.fitstore.catalog.catalogo.repository.ImagenRepository;
import com.fitstore.catalog.catalogo.repository.ProductoRepository;
import com.fitstore.catalog.catalogo.repository.ProductoSpecifications;
import com.fitstore.catalog.catalogo.repository.TallaRepository;
import com.fitstore.catalog.catalogo.repository.TramoDescuentoRepository;
import com.fitstore.catalog.catalogo.repository.VarianteRepository;
import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.FitstoreException;
import com.fitstore.common.api.PaginaResponse;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.common.storage.FileStorage;
import com.fitstore.common.storage.TipoBucket;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductoServiceImpl implements ProductoService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductoServiceImpl.class);

    private final ProductoRepository productos;
    private final VarianteRepository variantes;
    private final TallaRepository tallas;
    private final ColorRepository colores;
    private final CategoriaRepository categorias;
    private final ImagenRepository imagenes;
    private final TramoDescuentoRepository tramos;
    private final StockVariantePort stockPort;
    private final DisponibilidadPort disponibilidad;
    private final DescuentoService descuentos;
    private final CacheCatalogo cache;
    private final UrlsImagen urls;
    private final FileStorage almacenamiento;
    private final CatalogoMapper mapper;

    public ProductoServiceImpl(ProductoRepository productos, VarianteRepository variantes, TallaRepository tallas,
            ColorRepository colores, CategoriaRepository categorias, ImagenRepository imagenes,
            TramoDescuentoRepository tramos, StockVariantePort stockPort, DisponibilidadPort disponibilidad,
            DescuentoService descuentos, CacheCatalogo cache, UrlsImagen urls, FileStorage almacenamiento,
            CatalogoMapper mapper) {
        this.productos = productos;
        this.variantes = variantes;
        this.tallas = tallas;
        this.colores = colores;
        this.categorias = categorias;
        this.imagenes = imagenes;
        this.tramos = tramos;
        this.stockPort = stockPort;
        this.disponibilidad = disponibilidad;
        this.descuentos = descuentos;
        this.cache = cache;
        this.urls = urls;
        this.almacenamiento = almacenamiento;
        this.mapper = mapper;
    }

    // ---------------------------------------------------------------- lectura

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ProductoAdminResumenDto> listar(String q, EstadoProducto estado, Long categoriaId, Boolean destacado,
            Pageable paginacion) {
        Specification<Producto> spec = (producto, consulta, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            spec = spec.and(ProductoSpecifications.nombreOCodigoContiene(q.trim()));
        }
        if (estado != null) {
            spec = spec.and(ProductoSpecifications.conEstado(estado));
        }
        if (categoriaId != null) {
            spec = spec.and(ProductoSpecifications.deCategorias(List.of(categoriaId)));
        }
        if (destacado != null) {
            spec = spec.and(ProductoSpecifications.esDestacado(destacado));
        }
        Page<Producto> pagina = productos.findAll(spec, paginacion);
        List<Long> ids = pagina.getContent().stream().map(Producto::getId).toList();
        Map<Long, List<Variante>> variantesPorProducto = variantes.findByProductoIdIn(ids).stream()
                .collect(Collectors.groupingBy(Variante::getProductoId));
        Set<Long> varianteIds = variantesPorProducto.values().stream().flatMap(Collection::stream).map(Variante::getId)
                .collect(Collectors.toSet());
        Map<Long, Existencias> existencias = disponibilidad.existencias(varianteIds);
        Map<Long, Imagen> principales = imagenes.findByProductoIdInAndPrincipalTrue(ids).stream()
                .collect(Collectors.toMap(Imagen::getProductoId, Function.identity()));

        List<ProductoAdminResumenDto> filas = new ArrayList<>();
        for (Producto p : pagina.getContent()) {
            List<Variante> deProducto = variantesPorProducto.getOrDefault(p.getId(), List.of());
            int disponible = deProducto.stream().filter(Variante::isActivo)
                    .mapToInt(v -> existencias.getOrDefault(v.getId(), Existencias.VACIAS).disponible()).sum();
            Imagen principal = principales.get(p.getId());
            filas.add(new ProductoAdminResumenDto(p.getId(), p.getCodigo(), p.getNombre(), p.getSlug(), p.getEstado(),
                    p.isDestacado(), p.getPrecioDesde(), deProducto.size(), disponible,
                    principal == null ? null : urls.urlMiniatura(principal.getClave())));
        }
        return PaginaResponse.de(new PageImpl<>(filas, paginacion, pagina.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoAdminDto obtener(Long id) {
        return armar(buscar(id));
    }

    private Producto buscar(Long id) {
        return productos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos el producto"));
    }

    /** El producto completo como lo ve el formulario del backoffice (con números exactos de stock). */
    ProductoAdminDto armar(Producto p) {
        List<Variante> lista = variantes.findByProductoIdOrderByIdAsc(p.getId());
        Map<Long, Existencias> existencias = disponibilidad.existencias(lista.stream().map(Variante::getId).toList());
        Map<Long, Talla> tallaPorId = tallas.findAll().stream().collect(Collectors.toMap(Talla::getId, Function.identity()));
        Map<Long, Color> colorPorId = colores.findAll().stream().collect(Collectors.toMap(Color::getId, Function.identity()));
        List<VarianteAdminDto> variantesDto = lista.stream().map(v -> {
            Existencias e = existencias.getOrDefault(v.getId(), Existencias.VACIAS);
            return new VarianteAdminDto(v.getId(), v.getSku(), v.getTallaId(), tallaPorId.get(v.getTallaId()).getCodigo(),
                    v.getColorId(), colorPorId.get(v.getColorId()).getNombre(), v.getPrecio(), v.isActivo(), e.fisico(),
                    e.reservado(), e.disponible());
        }).toList();
        List<ImagenProductoDto> fotos = imagenes.findByProductoIdOrderByOrdenAsc(p.getId()).stream().map(urls::aDto).toList();
        List<TramoDto> propios = p.getModoDescuento() == ModoDescuento.PERSONALIZADO
                ? mapper.aTramosDto(tramos.findByProductoIdOrderByUnidadesMinimasAsc(p.getId())) : List.of();
        List<Long> categoriaIds = p.getCategorias().stream().map(Categoria::getId).sorted().toList();
        return new ProductoAdminDto(p.getId(), p.getCodigo(), p.getNombre(), p.getSlug(),
                p.getDescripcion() == null ? "" : p.getDescripcion(), p.getPrecioBase(), p.getEstado(), p.isDestacado(),
                p.getOrdenDestacado(), p.getModoDescuento(), propios, categoriaIds, variantesDto, fotos, p.getVersion());
    }

    // ---------------------------------------------------------------- producto

    @Override
    public ProductoAdminDto crear(ProductoRequest peticion) {
        if (productos.existsByCodigo(peticion.codigo())) {
            throw new ConflictoNegocioException("CODIGO_DUPLICADO", "Ya existe un producto con el código " + peticion.codigo());
        }
        Producto p = new Producto();
        p.setCodigo(peticion.codigo());
        p.setNombre(peticion.nombre().trim());
        p.setSlug(Slugs.unico(p.getNombre(), 180, productos::existsBySlug));
        p.setDescripcion(peticion.descripcion());
        p.setPrecioBase(peticion.precioBase());
        p.setPrecioDesde(peticion.precioBase());
        p.setEstado(EstadoProducto.BORRADOR);
        p.setCategorias(cargarCategorias(peticion.categoriaIds()));
        productos.saveAndFlush(p);
        return armar(p);
    }

    @Override
    public ProductoAdminDto editar(Long id, ProductoRequest peticion) {
        Producto p = buscar(id);
        if (peticion.version() == null) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION", "Falta la versión del producto que estás editando");
        }
        if (!peticion.version().equals(p.getVersion())) {
            throw new ConflictoNegocioException("CONFLICTO_VERSION", "Otro usuario modificó este producto; recarga la página");
        }
        if (!p.getCodigo().equals(peticion.codigo()) && productos.existsByCodigo(peticion.codigo())) {
            throw new ConflictoNegocioException("CODIGO_DUPLICADO", "Ya existe un producto con el código " + peticion.codigo());
        }
        p.setCodigo(peticion.codigo());
        p.setNombre(peticion.nombre().trim());
        p.setDescripcion(peticion.descripcion());
        p.setPrecioBase(peticion.precioBase());
        if (peticion.categoriaIds() != null) {
            p.setCategorias(cargarCategorias(peticion.categoriaIds()));
        }
        recalcularPrecioDesde(p);
        productos.saveAndFlush(p);
        cache.invalidarProducto(p.getSlug());
        return armar(p);
    }

    private Set<Categoria> cargarCategorias(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        Set<Long> pedidas = new LinkedHashSet<>(ids);
        List<Categoria> encontradas = categorias.findAllById(pedidas);
        if (encontradas.size() != pedidas.size()) {
            throw new RecursoNoEncontradoException("Alguna de las categorías no existe");
        }
        return new HashSet<>(encontradas);
    }

    @Override
    public void eliminar(Long id) {
        Producto p = buscar(id);
        List<Long> varianteIds = variantes.findByProductoIdOrderByIdAsc(id).stream().map(Variante::getId).toList();
        if (p.getEstado() != EstadoProducto.BORRADOR || stockPort.tieneMovimientosOReservas(varianteIds)) {
            throw new ConflictoNegocioException("PRODUCTO_NO_ELIMINABLE",
                    "Solo se eliminan productos en borrador sin movimientos ni reservas; usa Desactivar");
        }
        for (Imagen imagen : imagenes.findByProductoIdOrderByOrdenAsc(id)) {
            borrarArchivos(imagen.getClave());
        }
        imagenes.deleteByProductoId(id);
        tramos.deleteByProductoId(id);
        stockPort.eliminarStock(varianteIds);
        variantes.deleteAll(variantes.findByProductoIdOrderByIdAsc(id));
        variantes.flush();
        p.getCategorias().clear();
        productos.delete(p);
        productos.flush();
        cache.invalidarProducto(p.getSlug());
    }

    /** Los archivos se borran sin romper la operación: un archivo huérfano no debe impedir eliminar el producto. */
    private void borrarArchivos(String clave) {
        try {
            almacenamiento.eliminar(TipoBucket.PUBLICO, urls.claveGrande(clave));
            almacenamiento.eliminar(TipoBucket.PUBLICO, urls.claveMiniatura(clave));
        } catch (RuntimeException e) {
            LOG.warn("No se pudieron borrar los archivos de la imagen {}: {}", clave, e.getMessage());
        }
    }

    @Override
    public ProductoAdminDto publicar(Long id) {
        Producto p = buscar(id);
        List<String> faltantes = faltantesParaPublicar(p);
        if (!faltantes.isEmpty()) {
            throw new ConflictoNegocioException("NO_PUBLICABLE", "Falta completar el producto para publicarlo",
                    Map.of("faltantes", faltantes));
        }
        p.setEstado(EstadoProducto.ACTIVO);
        productos.saveAndFlush(p);
        cache.invalidarProducto(p.getSlug());
        return armar(p);
    }

    /** Invariante 4 del micro: precio > 0, una categoría, una variante activa y una imagen por cada color activo. */
    private List<String> faltantesParaPublicar(Producto p) {
        List<String> faltantes = new ArrayList<>();
        if (p.getCategorias().isEmpty()) {
            faltantes.add("Asigna al menos una categoría");
        }
        if (p.getPrecioBase().signum() <= 0) {
            faltantes.add("Define un precio mayor a 0");
        }
        List<Variante> activas = variantes.findByProductoIdOrderByIdAsc(p.getId()).stream().filter(Variante::isActivo).toList();
        if (activas.isEmpty()) {
            faltantes.add("Agrega al menos una variante activa");
            return faltantes;
        }
        for (Variante v : activas) {
            if (v.getPrecio() != null && v.getPrecio().signum() <= 0) {
                faltantes.add("Define un precio mayor a 0 en la variante " + v.getSku());
            }
        }
        Set<Long> coloresConFoto = imagenes.findByProductoIdOrderByOrdenAsc(p.getId()).stream().map(Imagen::getColorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Color> colorPorId = colores.findAll().stream().collect(Collectors.toMap(Color::getId, Function.identity()));
        activas.stream().map(Variante::getColorId).distinct().filter(c -> !coloresConFoto.contains(c)).sorted()
                .forEach(c -> faltantes.add("Agrega una imagen del color " + colorPorId.get(c).getNombre()));
        return faltantes;
    }

    @Override
    public ProductoAdminDto desactivar(Long id) {
        Producto p = buscar(id);
        if (p.getEstado() == EstadoProducto.BORRADOR) {
            throw new ConflictoNegocioException("ESTADO_INVALIDO", "Un producto en borrador no se desactiva; publícalo o elimínalo");
        }
        p.setEstado(EstadoProducto.INACTIVO);
        productos.saveAndFlush(p);
        cache.invalidarProducto(p.getSlug());
        return armar(p);
    }

    @Override
    public ProductoAdminDto asignarCategorias(Long id, List<Long> categoriaIds) {
        Producto p = buscar(id);
        p.setCategorias(cargarCategorias(categoriaIds));
        productos.saveAndFlush(p);
        cache.invalidarProducto(p.getSlug());
        return armar(p);
    }

    @Override
    public ProductoAdminDto marcarDestacado(Long id, DestacadoRequest peticion) {
        Producto p = buscar(id);
        p.setDestacado(peticion.destacado());
        p.setOrdenDestacado(peticion.destacado() ? peticion.orden() : null);
        productos.saveAndFlush(p);
        cache.invalidarProducto(p.getSlug());
        return armar(p);
    }

    @Override
    public ProductoAdminDto configurarDescuento(Long id, DescuentoProductoRequest peticion) {
        Producto p = buscar(id);
        descuentos.configurarProducto(p, peticion);
        productos.saveAndFlush(p);
        return armar(p);
    }

    // ---------------------------------------------------------------- variantes

    @Override
    public ProductoAdminDto crearVariante(Long productoId, VarianteRequest peticion) {
        Producto p = buscar(productoId);
        Talla talla = tallaActiva(peticion.tallaId());
        Color color = colorActivo(peticion.colorId());
        if (variantes.existsByProductoIdAndTallaIdAndColorId(productoId, talla.getId(), color.getId())) {
            throw new ConflictoNegocioException("VARIANTE_DUPLICADA", "Ya existe una variante de esa talla y color");
        }
        String sku = peticion.sku() == null || peticion.sku().isBlank() ? CodigosSku.generar(p.getCodigo(), color,
                colores.findAll(), talla) : peticion.sku().trim();
        if (variantes.existsBySku(sku)) {
            throw new ConflictoNegocioException("SKU_DUPLICADO", "Ya existe el SKU " + sku);
        }
        insertarVariante(p, talla, color, sku, peticion.precio());
        return terminarCambioDeVariantes(p);
    }

    @Override
    public ProductoAdminDto generarMatriz(Long productoId, MatrizRequest peticion) {
        Producto p = buscar(productoId);
        List<Talla> tallasPedidas = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(peticion.tallaIds())) {
            tallasPedidas.add(tallaActiva(id));
        }
        List<Color> coloresPedidos = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(peticion.colorIds())) {
            coloresPedidos.add(colorActivo(id));
        }
        List<Color> todosLosColores = colores.findAll();
        for (Color color : coloresPedidos) {
            for (Talla talla : tallasPedidas) {
                if (variantes.existsByProductoIdAndTallaIdAndColorId(productoId, talla.getId(), color.getId())) {
                    continue;
                }
                String sku = CodigosSku.generar(p.getCodigo(), color, todosLosColores, talla);
                if (variantes.existsBySku(sku)) {
                    throw new ConflictoNegocioException("SKU_DUPLICADO", "Ya existe el SKU " + sku);
                }
                insertarVariante(p, talla, color, sku, null);
            }
        }
        return terminarCambioDeVariantes(p);
    }

    private Talla tallaActiva(Long id) {
        Talla talla = tallas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos la talla " + id));
        if (!talla.isActivo()) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION", "La talla " + talla.getCodigo() + " está desactivada");
        }
        return talla;
    }

    private Color colorActivo(Long id) {
        Color color = colores.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos el color " + id));
        if (!color.isActivo()) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION", "El color " + color.getNombre() + " está desactivado");
        }
        return color;
    }

    private void insertarVariante(Producto p, Talla talla, Color color, String sku, BigDecimal precio) {
        Variante v = new Variante();
        v.setProductoId(p.getId());
        v.setTallaId(talla.getId());
        v.setColorId(color.getId());
        v.setSku(sku);
        v.setPrecio(precio);
        variantes.saveAndFlush(v);
        stockPort.crearStock(v.getId());
    }

    private ProductoAdminDto terminarCambioDeVariantes(Producto p) {
        recalcularPrecioDesde(p);
        productos.saveAndFlush(p);
        cache.invalidarProducto(p.getSlug());
        return armar(p);
    }

    @Override
    public ProductoAdminDto editarVariante(Long varianteId, EditarVarianteRequest peticion) {
        Variante v = buscarVariante(varianteId);
        String sku = peticion.sku().trim();
        if (!sku.equals(v.getSku()) && variantes.existsBySku(sku)) {
            throw new ConflictoNegocioException("SKU_DUPLICADO", "Ya existe el SKU " + sku);
        }
        v.setSku(sku);
        v.setPrecio(peticion.precio());
        variantes.saveAndFlush(v);
        return terminarCambioDeVariantes(buscar(v.getProductoId()));
    }

    @Override
    public void eliminarVariante(Long varianteId) {
        Variante v = buscarVariante(varianteId);
        if (stockPort.tieneMovimientosOReservas(List.of(varianteId))) {
            throw new ConflictoNegocioException("VARIANTE_NO_ELIMINABLE",
                    "La variante tiene movimientos o reservas; desactívala en su lugar");
        }
        Producto p = buscar(v.getProductoId());
        stockPort.eliminarStock(List.of(varianteId));
        variantes.delete(v);
        variantes.flush();
        terminarCambioDeVariantes(p);
    }

    @Override
    public ProductoAdminDto desactivarVariante(Long varianteId) {
        return cambiarActivo(varianteId, false);
    }

    @Override
    public ProductoAdminDto activarVariante(Long varianteId) {
        return cambiarActivo(varianteId, true);
    }

    private ProductoAdminDto cambiarActivo(Long varianteId, boolean activo) {
        Variante v = buscarVariante(varianteId);
        v.setActivo(activo);
        variantes.saveAndFlush(v);
        return terminarCambioDeVariantes(buscar(v.getProductoId()));
    }

    private Variante buscarVariante(Long id) {
        return variantes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos la variante"));
    }

    /** "Desde" = el menor precio de las variantes activas (D4); sin variantes activas, el precio base. */
    private void recalcularPrecioDesde(Producto p) {
        BigDecimal menor = variantes.findByProductoIdOrderByIdAsc(p.getId()).stream().filter(Variante::isActivo)
                .map(v -> v.getPrecio() != null ? v.getPrecio() : p.getPrecioBase())
                .min(Comparator.naturalOrder()).orElse(p.getPrecioBase());
        p.setPrecioDesde(menor);
    }
}
