package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.ProductoCreadoDto;
import com.fitstore.catalog.catalogo.dto.ResultadoCargaDto;
import com.fitstore.catalog.catalogo.dto.ValidacionCargaDto;
import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.Color;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Talla;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.catalogo.repository.CategoriaRepository;
import com.fitstore.catalog.catalogo.repository.ColorRepository;
import com.fitstore.catalog.catalogo.repository.ProductoRepository;
import com.fitstore.catalog.catalogo.repository.TallaRepository;
import com.fitstore.catalog.catalogo.repository.VarianteRepository;
import com.fitstore.catalog.catalogo.service.carga.AlmacenValidaciones;
import com.fitstore.catalog.catalogo.service.carga.FilaCarga;
import com.fitstore.catalog.catalogo.service.carga.LectorCarga;
import com.fitstore.catalog.catalogo.service.carga.ModeloCarga.ProductoCarga;
import com.fitstore.catalog.catalogo.service.carga.ModeloCarga.ValidacionGuardada;
import com.fitstore.catalog.catalogo.service.carga.ModeloCarga.VarianteCarga;
import com.fitstore.catalog.catalogo.service.carga.PlantillaCarga;
import com.fitstore.catalog.catalogo.service.carga.ValidadorCarga;
import com.fitstore.catalog.config.CatalogoProperties;
import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.FitstoreException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CargaMasivaServiceImpl implements CargaMasivaService {

    private static final long MAXIMO_BYTES = 5L * 1024 * 1024;
    private static final String MOTIVO_ENTRADA = "Carga masiva";

    private final ProductoRepository productos;
    private final VarianteRepository variantes;
    private final TallaRepository tallas;
    private final ColorRepository colores;
    private final CategoriaRepository categorias;
    private final StockVariantePort stockPort;
    private final AlmacenValidaciones almacen;
    private final CatalogoProperties propiedades;

    public CargaMasivaServiceImpl(ProductoRepository productos, VarianteRepository variantes, TallaRepository tallas,
            ColorRepository colores, CategoriaRepository categorias, StockVariantePort stockPort, AlmacenValidaciones almacen,
            CatalogoProperties propiedades) {
        this.productos = productos;
        this.variantes = variantes;
        this.tallas = tallas;
        this.colores = colores;
        this.categorias = categorias;
        this.stockPort = stockPort;
        this.almacen = almacen;
        this.propiedades = propiedades;
    }

    @Override
    public Archivo plantilla(String formato) {
        if ("csv".equalsIgnoreCase(formato)) {
            return new Archivo(PlantillaCarga.csv(), "text/csv", "plantilla-productos.csv");
        }
        if ("xlsx".equalsIgnoreCase(formato)) {
            return new Archivo(PlantillaCarga.xlsx(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "plantilla-productos.xlsx");
        }
        throw new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION", "El formato debe ser xlsx o csv");
    }

    // ---------------------------------------------------------------- validar

    @Override
    @Transactional(readOnly = true)
    public ValidacionCargaDto validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION", "Elige un archivo CSV o XLSX");
        }
        if (archivo.getSize() > MAXIMO_BYTES) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "ARCHIVO_MUY_GRANDE", "El archivo supera los 5 MB permitidos");
        }
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION", "No se pudo leer el archivo");
        }
        List<FilaCarga> filas = LectorCarga.leer(archivo.getOriginalFilename(), contenido, propiedades.cargaMasiva().maxFilas());

        Set<String> codigos = filas.stream().map(f -> f.valor("codigo").toUpperCase()).filter(c -> !c.isEmpty())
                .collect(Collectors.toSet());
        Set<String> existentes = codigos.isEmpty() ? Set.of() : new HashSet<>(productos.codigosExistentes(codigos));
        ValidadorCarga validador = new ValidadorCarga(
                tallas.findByActivoTrueOrderByOrdenAsc(), colores.findByActivoTrueOrderByIdAsc(),
                categorias.findAllByOrderByOrdenAscNombreAsc().stream().filter(Categoria::isActivo).toList(), existentes);
        ValidadorCarga.Resultado resultado = validador.validar(filas);

        ValidacionGuardada guardada = new ValidacionGuardada(UUID.randomUUID(), Instant.now().plus(almacen.vigencia()),
                filas.size(), resultado.errores(), resultado.productos());
        almacen.guardar(guardada);
        return new ValidacionCargaDto(guardada.id(), guardada.valida(), filas.size(), resultado.productos().size(),
                resultado.variantes(), resultado.errores(), guardada.expiraEn());
    }

    // ---------------------------------------------------------------- confirmar

    @Override
    @Transactional
    public ResultadoCargaDto confirmar(UUID validacionId, UUID usuarioId) {
        ValidacionGuardada validacion = almacen.obtener(validacionId).orElseThrow(() -> new ConflictoNegocioException(
                "VALIDACION_VENCIDA", "La validación venció (dura 30 minutos) o ya se usó; sube el archivo de nuevo"));
        if (!validacion.valida()) {
            throw new ConflictoNegocioException("VALIDACION_CON_ERRORES",
                    "El informe tiene errores; corrige el archivo y vuelve a validarlo");
        }
        verificarQueSigueLibre(validacion);

        Map<Long, Talla> tallaPorId = tallas.findAll().stream().collect(Collectors.toMap(Talla::getId, Function.identity()));
        List<Color> todosLosColores = colores.findAll();
        Map<Long, Color> colorPorId = todosLosColores.stream().collect(Collectors.toMap(Color::getId, Function.identity()));
        verificarSkus(validacion, tallaPorId, colorPorId, todosLosColores);

        List<ProductoCreadoDto> creados = new ArrayList<>();
        int totalVariantes = 0;
        int entradas = 0;
        for (ProductoCarga datos : validacion.productos()) {
            Producto producto = new Producto();
            producto.setCodigo(datos.codigo());
            producto.setNombre(datos.nombre());
            producto.setSlug(Slugs.unico(datos.nombre(), 180, productos::existsBySlug));
            producto.setDescripcion(datos.descripcion());
            producto.setPrecioBase(datos.precioBase());
            producto.setEstado(EstadoProducto.BORRADOR);
            producto.setCategorias(new HashSet<>(categorias.findAllById(datos.categoriaIds())));
            BigDecimal menor = datos.precioBase();
            for (VarianteCarga v : datos.variantes()) {
                BigDecimal precio = v.precio() != null ? v.precio() : datos.precioBase();
                menor = precio.compareTo(menor) < 0 ? precio : menor;
            }
            producto.setPrecioDesde(menor);
            productos.saveAndFlush(producto);

            for (VarianteCarga v : datos.variantes()) {
                Variante variante = new Variante();
                variante.setProductoId(producto.getId());
                variante.setTallaId(v.tallaId());
                variante.setColorId(v.colorId());
                variante.setPrecio(v.precio());
                variante.setSku(CodigosSku.generar(datos.codigo(), colorPorId.get(v.colorId()), todosLosColores,
                        tallaPorId.get(v.tallaId())));
                variantes.saveAndFlush(variante);
                stockPort.crearStockConEntrada(variante.getId(), v.stockInicial(), MOTIVO_ENTRADA, usuarioId);
                totalVariantes++;
                if (v.stockInicial() > 0) {
                    entradas++;
                }
            }
            creados.add(new ProductoCreadoDto(producto.getId(), producto.getCodigo(), producto.getNombre()));
        }
        almacen.eliminar(validacionId);
        return new ResultadoCargaDto(creados, totalVariantes, entradas);
    }

    /** Entre validar y confirmar alguien pudo crear uno de los códigos: se revisa antes de crear nada. */
    private void verificarQueSigueLibre(ValidacionGuardada validacion) {
        List<String> codigos = validacion.productos().stream().map(ProductoCarga::codigo).toList();
        List<String> ocupados = productos.codigosExistentes(codigos).stream().sorted().toList();
        if (!ocupados.isEmpty()) {
            throw new ConflictoNegocioException("CODIGO_DUPLICADO",
                    "Estos códigos ya existen: " + String.join(", ", ocupados) + ". Valida el archivo de nuevo",
                    Map.of("codigos", ocupados));
        }
    }

    private void verificarSkus(ValidacionGuardada validacion, Map<Long, Talla> tallaPorId, Map<Long, Color> colorPorId,
            List<Color> todosLosColores) {
        Set<String> vistos = new HashSet<>();
        List<String> choques = new ArrayList<>();
        for (ProductoCarga p : validacion.productos()) {
            for (VarianteCarga v : p.variantes()) {
                String sku = CodigosSku.generar(p.codigo(), colorPorId.get(v.colorId()), todosLosColores, tallaPorId.get(v.tallaId()));
                if (!vistos.add(sku) || variantes.existsBySku(sku)) {
                    choques.add(sku);
                }
            }
        }
        if (!choques.isEmpty()) {
            choques.sort(Comparator.naturalOrder());
            throw new ConflictoNegocioException("SKU_DUPLICADO", "Estos SKU ya existen: " + String.join(", ", choques));
        }
    }
}
