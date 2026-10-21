package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.CategoriaHijaDto;
import com.fitstore.catalog.catalogo.dto.CategoriaNodoDto;
import com.fitstore.catalog.catalogo.dto.ColorDetalleDto;
import com.fitstore.catalog.catalogo.dto.ColorDto;
import com.fitstore.catalog.catalogo.dto.Disponibilidad;
import com.fitstore.catalog.catalogo.dto.GuiaTallasDto;
import com.fitstore.catalog.catalogo.dto.ImagenProductoDto;
import com.fitstore.catalog.catalogo.dto.ProductoDetalleDto;
import com.fitstore.catalog.catalogo.dto.ProductoTarjetaDto;
import com.fitstore.catalog.catalogo.dto.TallaDto;
import com.fitstore.catalog.catalogo.dto.TramoDto;
import com.fitstore.catalog.catalogo.dto.VariantePublicaDto;
import com.fitstore.catalog.catalogo.mapper.CatalogoMapper;
import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.Color;
import com.fitstore.catalog.catalogo.model.GuiaTallas;
import com.fitstore.catalog.catalogo.model.Imagen;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Talla;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.catalogo.repository.CategoriaRepository;
import com.fitstore.catalog.catalogo.repository.ColorRepository;
import com.fitstore.catalog.catalogo.repository.GuiaTallasRepository;
import com.fitstore.catalog.catalogo.repository.ImagenRepository;
import com.fitstore.catalog.catalogo.repository.ProductoRepository;
import com.fitstore.catalog.catalogo.repository.ProductoSpecifications;
import com.fitstore.catalog.catalogo.repository.TallaRepository;
import com.fitstore.catalog.catalogo.repository.VarianteRepository;
import com.fitstore.catalog.catalogo.service.dominio.PoliticaDescuentos;
import com.fitstore.catalog.catalogo.service.dominio.Tramo;
import com.fitstore.catalog.config.InventarioProperties;
import com.fitstore.common.api.PaginaResponse;
import com.fitstore.common.api.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

@Service
@Transactional(readOnly = true)
public class CatalogoPublicoServiceImpl implements CatalogoPublicoService {

    private static final int MAX_DESTACADOS = 8;

    private final ProductoRepository productos;
    private final VarianteRepository variantes;
    private final TallaRepository tallas;
    private final ColorRepository colores;
    private final CategoriaRepository categorias;
    private final GuiaTallasRepository guias;
    private final ImagenRepository imagenes;
    private final DisponibilidadPort disponibilidad;
    private final DescuentoService descuentos;
    private final CacheCatalogo cache;
    private final UrlsImagen urls;
    private final CatalogoMapper mapper;
    private final InventarioProperties inventario;

    public CatalogoPublicoServiceImpl(ProductoRepository productos, VarianteRepository variantes, TallaRepository tallas,
            ColorRepository colores, CategoriaRepository categorias, GuiaTallasRepository guias, ImagenRepository imagenes,
            DisponibilidadPort disponibilidad, DescuentoService descuentos, CacheCatalogo cache, UrlsImagen urls,
            CatalogoMapper mapper, InventarioProperties inventario) {
        this.productos = productos;
        this.variantes = variantes;
        this.tallas = tallas;
        this.colores = colores;
        this.categorias = categorias;
        this.guias = guias;
        this.imagenes = imagenes;
        this.disponibilidad = disponibilidad;
        this.descuentos = descuentos;
        this.cache = cache;
        this.urls = urls;
        this.mapper = mapper;
        this.inventario = inventario;
    }

    // ---------------------------------------------------------------- menú y guía

    @Override
    public List<CategoriaNodoDto> arbolCategorias() {
        return cache.obtener(CacheCatalogo.ARBOL_CATEGORIAS, new TypeReference<List<CategoriaNodoDto>>() { }, () -> {
            List<Categoria> activas = categorias.findAllByOrderByOrdenAscNombreAsc().stream().filter(Categoria::isActivo).toList();
            Map<Long, List<Categoria>> hijasPorPadre = activas.stream().filter(c -> c.getPadreId() != null)
                    .collect(Collectors.groupingBy(Categoria::getPadreId));
            return activas.stream().filter(c -> c.getPadreId() == null)
                    .map(raiz -> new CategoriaNodoDto(raiz.getId(), raiz.getNombre(), raiz.getSlug(),
                            hijasPorPadre.getOrDefault(raiz.getId(), List.of()).stream().map(mapper::aHijaDto).toList()))
                    .toList();
        });
    }

    @Override
    public GuiaTallasDto guiaTallas(String slugCategoria) {
        Categoria categoria = categorias.findBySlug(slugCategoria)
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos la categoría"));
        return guiaHeredada(categoria).map(GuiaTallasConversor::aDto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Esta categoría no tiene guía de tallas"));
    }

    /** La guía de la categoría o, si no tiene, la de su padre (D6, CA-2-10). */
    private Optional<GuiaTallas> guiaHeredada(Categoria categoria) {
        Optional<GuiaTallas> propia = guias.findByCategoriaId(categoria.getId());
        if (propia.isPresent() || categoria.getPadreId() == null) {
            return propia;
        }
        return guias.findByCategoriaId(categoria.getPadreId());
    }

    // ---------------------------------------------------------------- listado

    @Override
    public PaginaResponse<ProductoTarjetaDto> listar(FiltroProductos filtro, Pageable paginacion) {
        Page<Producto> pagina = productos.findAll(especificacion(filtro), paginacion);
        List<ProductoTarjetaDto> tarjetas = tarjetas(pagina.getContent());
        return PaginaResponse.de(new org.springframework.data.domain.PageImpl<>(tarjetas, paginacion, pagina.getTotalElements()));
    }

    private Specification<Producto> especificacion(FiltroProductos f) {
        Specification<Producto> spec = ProductoSpecifications.visibleEnTienda();
        if (f.q() != null && !f.q().isBlank()) {
            spec = spec.and(ProductoSpecifications.textoEnNombreODescripcion(f.q().trim()));
        }
        if (f.categoria() != null && !f.categoria().isBlank()) {
            Optional<Categoria> categoria = categorias.findBySlug(f.categoria());
            spec = spec.and(categoria.isPresent()
                    ? ProductoSpecifications.deCategorias(categorias.idsConHijas(categoria.get().getId()))
                    : ProductoSpecifications.sinResultados());
        }
        Set<Long> tallaIds = f.tallas().stream().map(tallas::findByCodigoIgnoreCase).flatMap(Optional::stream)
                .map(Talla::getId).collect(Collectors.toSet());
        if (!f.tallas().isEmpty() && tallaIds.isEmpty()) {
            return spec.and(ProductoSpecifications.sinResultados());
        }
        if (!tallaIds.isEmpty() || !f.colores().isEmpty() || f.soloDisponibles()) {
            spec = spec.and(ProductoSpecifications.conVarianteQueCumple(tallaIds, f.colores(), f.soloDisponibles()));
        }
        if (f.precioMin() != null || f.precioMax() != null) {
            spec = spec.and(ProductoSpecifications.precioEntre(f.precioMin(), f.precioMax()));
        }
        return spec;
    }

    @Override
    public List<ProductoTarjetaDto> destacados() {
        List<Long> ids = cache.obtener(CacheCatalogo.DESTACADOS, new TypeReference<List<Long>>() { }, () -> {
            Specification<Producto> spec = ProductoSpecifications.visibleEnTienda().and(ProductoSpecifications.esDestacado(true));
            Sort orden = Sort.by(Sort.Order.asc("ordenDestacado").nullsLast(), Sort.Order.asc("id"));
            return productos.findAll(spec, PageRequest.of(0, MAX_DESTACADOS, orden)).map(Producto::getId).toList();
        });
        Map<Long, Producto> porId = productos.findAllById(ids).stream().collect(Collectors.toMap(Producto::getId, Function.identity()));
        return tarjetas(ids.stream().map(porId::get).filter(p -> p != null
                && p.getEstado() == com.fitstore.catalog.catalogo.model.EstadoProducto.ACTIVO).toList());
    }

    /** Arma las tarjetas de una página con consultas por lote (sin una consulta por producto). */
    private List<ProductoTarjetaDto> tarjetas(List<Producto> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        List<Long> ids = lista.stream().map(Producto::getId).toList();
        Map<Long, List<Variante>> variantesPorProducto = variantes.findByProductoIdInAndActivoTrue(ids).stream()
                .collect(Collectors.groupingBy(Variante::getProductoId));
        Set<Long> varianteIds = variantesPorProducto.values().stream().flatMap(Collection::stream).map(Variante::getId)
                .collect(Collectors.toSet());
        Map<Long, Integer> disponible = disponibilidad.disponibles(varianteIds);
        Map<Long, Color> coloresPorId = colores.findAll().stream().collect(Collectors.toMap(Color::getId, Function.identity()));
        Map<Long, Imagen> principales = imagenes.findByProductoIdInAndPrincipalTrue(ids).stream()
                .collect(Collectors.toMap(Imagen::getProductoId, Function.identity()));
        PoliticaDescuentos politica = descuentos.politicaPara(lista);

        List<ProductoTarjetaDto> resultado = new ArrayList<>();
        for (Producto p : lista) {
            List<Variante> deProducto = variantesPorProducto.getOrDefault(p.getId(), List.of());
            List<ColorDto> coloresConStock = deProducto.stream().filter(v -> disponible.getOrDefault(v.getId(), 0) > 0)
                    .map(Variante::getColorId).distinct().sorted().map(coloresPorId::get).map(mapper::aDto).toList();
            Imagen principal = principales.get(p.getId());
            resultado.add(new ProductoTarjetaDto(p.getId(), p.getSlug(), p.getNombre(),
                    principal == null ? null : urls.urlMiniatura(principal.getClave()), p.getPrecioDesde(), coloresConStock,
                    etiqueta(politica.tramosDe(p.getId())), coloresConStock.isEmpty()));
        }
        return resultado;
    }

    /** "Desde 3 un. −5 %" con el primer tramo del producto (CA-2-05); sin tramos no hay etiqueta. */
    private static String etiqueta(List<Tramo> tramos) {
        if (tramos.isEmpty()) {
            return null;
        }
        Tramo primero = tramos.get(0);
        String porcentaje = primero.porcentaje().stripTrailingZeros().toPlainString().replace('.', ',');
        return "Desde " + primero.unidadesMinimas() + " un. −" + porcentaje + " %";
    }

    // ---------------------------------------------------------------- detalle

    /** Parte del detalle que se guarda en la caché: nunca incluye el stock. */
    public record DetalleBase(Long id, String slug, String nombre, String descripcion, BigDecimal precioDesde,
            List<CategoriaHijaDto> categorias, List<TallaDto> tallas, List<ColorDto> colores,
            List<VarianteBase> variantes, List<ImagenProductoDto> imagenes, List<TramoDto> tramos, boolean tieneGuiaTallas) {
    }

    public record VarianteBase(Long id, String sku, Long tallaId, Long colorId, BigDecimal precio) {
    }

    @Override
    public ProductoDetalleDto detalle(String slug) {
        DetalleBase base = cache.obtener(CacheCatalogo.clavePorSlug(slug), new TypeReference<DetalleBase>() { },
                () -> construirBase(slug));
        Map<Long, Integer> disponible = disponibilidad.disponibles(base.variantes().stream().map(VarianteBase::id).toList());
        int umbral = inventario.umbralUltimasUnidades();

        List<VariantePublicaDto> variantesPublicas = base.variantes().stream().map(v -> {
            int d = disponible.getOrDefault(v.id(), 0);
            Disponibilidad nivel = d <= 0 ? Disponibilidad.AGOTADO : d <= umbral ? Disponibilidad.ULTIMAS : Disponibilidad.DISPONIBLE;
            return new VariantePublicaDto(v.id(), v.sku(), v.tallaId(), v.colorId(), v.precio(), nivel,
                    nivel == Disponibilidad.ULTIMAS ? d : null);
        }).toList();
        Set<Long> coloresConStock = base.variantes().stream().filter(v -> disponible.getOrDefault(v.id(), 0) > 0)
                .map(VarianteBase::colorId).collect(Collectors.toSet());
        List<ColorDetalleDto> coloresDetalle = base.colores().stream()
                .map(c -> new ColorDetalleDto(c.id(), c.nombre(), c.hex(), c.activo(), coloresConStock.contains(c.id()))).toList();
        return new ProductoDetalleDto(base.id(), base.slug(), base.nombre(), base.descripcion(), base.precioDesde(),
                base.categorias(), base.tallas(), coloresDetalle, variantesPublicas, base.imagenes(), base.tramos(),
                base.tieneGuiaTallas());
    }

    private DetalleBase construirBase(String slug) {
        Producto producto = productos.findBySlug(slug)
                .filter(p -> p.getEstado() == com.fitstore.catalog.catalogo.model.EstadoProducto.ACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos el producto"));
        List<Variante> activas = variantes.findByProductoIdOrderByIdAsc(producto.getId()).stream().filter(Variante::isActivo).toList();
        if (activas.isEmpty()) {
            throw new RecursoNoEncontradoException("No encontramos el producto");
        }
        Map<Long, Talla> tallasPorId = tallas.findAll().stream().collect(Collectors.toMap(Talla::getId, Function.identity()));
        Map<Long, Color> coloresPorId = colores.findAll().stream().collect(Collectors.toMap(Color::getId, Function.identity()));

        List<TallaDto> tallasDelProducto = activas.stream().map(Variante::getTallaId).distinct().map(tallasPorId::get)
                .sorted(Comparator.comparingInt(Talla::getOrden)).map(mapper::aDto).toList();
        List<ColorDto> coloresDelProducto = activas.stream().map(Variante::getColorId).distinct().sorted()
                .map(coloresPorId::get).map(mapper::aDto).toList();
        List<VarianteBase> variantesBase = activas.stream().map(v -> new VarianteBase(v.getId(), v.getSku(), v.getTallaId(),
                v.getColorId(), v.getPrecio() != null ? v.getPrecio() : producto.getPrecioBase())).toList();
        List<ImagenProductoDto> fotos = imagenes.findByProductoIdOrderByOrdenAsc(producto.getId()).stream().map(urls::aDto).toList();
        List<TramoDto> tramos = descuentos.politicaPara(List.of(producto)).tramosDe(producto.getId()).stream().map(mapper::aDto).toList();
        List<Categoria> deProducto = producto.getCategorias().stream().sorted(Comparator.comparing(Categoria::getNombre)).toList();
        boolean conGuia = deProducto.stream().anyMatch(c -> guiaHeredada(c).isPresent());
        return new DetalleBase(producto.getId(), producto.getSlug(), producto.getNombre(),
                producto.getDescripcion() == null ? "" : producto.getDescripcion(), producto.getPrecioDesde(),
                deProducto.stream().map(mapper::aHijaDto).toList(), tallasDelProducto, coloresDelProducto, variantesBase, fotos,
                tramos, conGuia);
    }

    // ---------------------------------------------------------------- filtros

    @Override
    public List<TallaDto> tallas() {
        return mapper.aTallasDto(tallas.findByActivoTrueOrderByOrdenAsc());
    }

    @Override
    public List<ColorDto> colores() {
        return mapper.aColoresDto(colores.findByActivoTrueOrderByIdAsc());
    }
}
