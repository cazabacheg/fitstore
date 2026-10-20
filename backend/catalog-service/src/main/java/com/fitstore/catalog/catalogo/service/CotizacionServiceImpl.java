package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.CotizacionDto;
import com.fitstore.catalog.catalogo.dto.EstadoLinea;
import com.fitstore.catalog.catalogo.dto.LineaCotizadaDto;
import com.fitstore.catalog.catalogo.dto.LineaCotizarRequest;
import com.fitstore.catalog.catalogo.dto.ProductoCotizadoDto;
import com.fitstore.catalog.catalogo.dto.SiguienteTramoDto;
import com.fitstore.catalog.catalogo.dto.TramoDto;
import com.fitstore.catalog.catalogo.model.Color;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.model.Imagen;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Talla;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.catalogo.repository.ColorRepository;
import com.fitstore.catalog.catalogo.repository.ImagenRepository;
import com.fitstore.catalog.catalogo.repository.ProductoRepository;
import com.fitstore.catalog.catalogo.repository.TallaRepository;
import com.fitstore.catalog.catalogo.repository.VarianteRepository;
import com.fitstore.catalog.catalogo.service.dominio.CalculadoraDescuentos;
import com.fitstore.catalog.catalogo.service.dominio.Cotizacion;
import com.fitstore.catalog.catalogo.service.dominio.LineaCalculada;
import com.fitstore.catalog.catalogo.service.dominio.LineaCotizable;
import com.fitstore.catalog.catalogo.service.dominio.PoliticaDescuentos;
import com.fitstore.catalog.catalogo.service.dominio.ResumenProducto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CotizacionServiceImpl implements CotizacionService {

    private static final BigDecimal CERO = new BigDecimal("0.00");

    private final VarianteRepository variantes;
    private final ProductoRepository productos;
    private final TallaRepository tallas;
    private final ColorRepository colores;
    private final ImagenRepository imagenes;
    private final DisponibilidadPort disponibilidad;
    private final DescuentoService descuentos;
    private final UrlsImagen urls;
    private final CalculadoraDescuentos calculadora = new CalculadoraDescuentos();

    public CotizacionServiceImpl(VarianteRepository variantes, ProductoRepository productos, TallaRepository tallas,
            ColorRepository colores, ImagenRepository imagenes, DisponibilidadPort disponibilidad,
            DescuentoService descuentos, UrlsImagen urls) {
        this.variantes = variantes;
        this.productos = productos;
        this.tallas = tallas;
        this.colores = colores;
        this.imagenes = imagenes;
        this.disponibilidad = disponibilidad;
        this.descuentos = descuentos;
        this.urls = urls;
    }

    @Override
    public CotizacionDto cotizar(List<LineaCotizarRequest> pedidas) {
        Set<Long> varianteIds = pedidas.stream().map(LineaCotizarRequest::varianteId).collect(Collectors.toSet());
        Map<Long, Variante> variantePorId = variantes.findAllById(varianteIds).stream()
                .collect(Collectors.toMap(Variante::getId, Function.identity()));
        Set<Long> productoIds = variantePorId.values().stream().map(Variante::getProductoId).collect(Collectors.toSet());
        Map<Long, Producto> productoPorId = productos.findAllById(productoIds).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));
        Map<Long, Talla> tallaPorId = tallas.findAll().stream().collect(Collectors.toMap(Talla::getId, Function.identity()));
        Map<Long, Color> colorPorId = colores.findAll().stream().collect(Collectors.toMap(Color::getId, Function.identity()));
        Map<Long, Imagen> principal = imagenes.findByProductoIdInAndPrincipalTrue(productoIds).stream()
                .collect(Collectors.toMap(Imagen::getProductoId, Function.identity()));
        Map<Long, Integer> disponible = disponibilidad.disponibles(varianteIds);

        // Solo las líneas disponibles entran al cálculo, en el mismo orden en que llegaron
        List<LineaCotizable> calculables = new ArrayList<>();
        for (LineaCotizarRequest pedida : pedidas) {
            Variante v = variantePorId.get(pedida.varianteId());
            if (estaDisponible(v, productoPorId)) {
                BigDecimal precio = v.getPrecio() != null ? v.getPrecio() : productoPorId.get(v.getProductoId()).getPrecioBase();
                calculables.add(new LineaCotizable(v.getId(), v.getProductoId(), precio, pedida.cantidad()));
            }
        }
        List<Producto> activos = calculables.stream().map(l -> productoPorId.get(l.productoId())).distinct().toList();
        PoliticaDescuentos politica = descuentos.politicaPara(activos);
        Cotizacion calculo = calculadora.calcular(calculables, politica);

        List<LineaCotizadaDto> lineas = new ArrayList<>();
        int siguiente = 0;
        for (LineaCotizarRequest pedida : pedidas) {
            Variante v = variantePorId.get(pedida.varianteId());
            Producto p = v == null ? null : productoPorId.get(v.getProductoId());
            String talla = v == null ? null : tallaPorId.get(v.getTallaId()).getCodigo();
            String color = v == null ? null : colorPorId.get(v.getColorId()).getNombre();
            String imagen = p == null || !principal.containsKey(p.getId()) ? null : urls.urlMiniatura(principal.get(p.getId()).getClave());
            if (!estaDisponible(v, productoPorId)) {
                lineas.add(new LineaCotizadaDto(pedida.varianteId(), p == null ? null : p.getId(), v == null ? null : v.getSku(),
                        p == null ? null : p.getNombre(), p == null ? null : p.getSlug(), talla, color, imagen, CERO,
                        pedida.cantidad(), CERO, CERO, CERO, CERO, EstadoLinea.NO_DISPONIBLE, null));
                continue;
            }
            LineaCalculada c = calculo.lineas().get(siguiente++);
            int quedan = disponible.getOrDefault(v.getId(), 0);
            boolean alcanza = pedida.cantidad() <= quedan;
            lineas.add(new LineaCotizadaDto(v.getId(), p.getId(), v.getSku(), p.getNombre(), p.getSlug(), talla, color, imagen,
                    c.precioUnitario(), c.cantidad(), c.subtotal(), c.descuentoPorcentaje(), c.descuentoMonto(), c.total(),
                    alcanza ? EstadoLinea.OK : EstadoLinea.STOCK_INSUFICIENTE, alcanza ? null : Math.max(0, quedan)));
        }

        List<ProductoCotizadoDto> resumen = calculo.productos().stream().map(r -> aDto(r, productoPorId.get(r.productoId()))).toList();
        return new CotizacionDto(lineas, resumen, calculo.subtotal(), calculo.descuentoTotal(), calculo.total());
    }

    private static boolean estaDisponible(Variante v, Map<Long, Producto> productos) {
        if (v == null || !v.isActivo()) {
            return false;
        }
        Producto p = productos.get(v.getProductoId());
        return p != null && p.getEstado() == EstadoProducto.ACTIVO;
    }

    private static ProductoCotizadoDto aDto(ResumenProducto r, Producto p) {
        TramoDto alcanzado = r.tramoAlcanzado() == null ? null
                : new TramoDto(r.tramoAlcanzado().unidadesMinimas(), r.tramoAlcanzado().porcentaje());
        SiguienteTramoDto siguiente = r.siguienteTramo() == null ? null
                : new SiguienteTramoDto(r.siguienteTramo().unidadesMinimas(), r.siguienteTramo().porcentaje(), r.faltan());
        return new ProductoCotizadoDto(r.productoId(), p.getNombre(), r.unidades(), alcanzado, siguiente);
    }
}
