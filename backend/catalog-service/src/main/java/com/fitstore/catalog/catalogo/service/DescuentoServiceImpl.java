package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.DescuentoProductoRequest;
import com.fitstore.catalog.catalogo.dto.PoliticaDescuentosDto;
import com.fitstore.catalog.catalogo.dto.TramoDto;
import com.fitstore.catalog.catalogo.mapper.CatalogoMapper;
import com.fitstore.catalog.catalogo.model.ConfiguracionDescuento;
import com.fitstore.catalog.catalogo.model.ModoDescuento;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.TramoDescuento;
import com.fitstore.catalog.catalogo.repository.ConfiguracionDescuentoRepository;
import com.fitstore.catalog.catalogo.repository.TramoDescuentoRepository;
import com.fitstore.catalog.catalogo.service.dominio.PoliticaDescuentos;
import com.fitstore.catalog.config.CatalogoProperties;
import com.fitstore.catalog.catalogo.service.dominio.Tramo;
import com.fitstore.common.api.FitstoreException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

@Service
public class DescuentoServiceImpl implements DescuentoService {

    private final ConfiguracionDescuentoRepository configuracion;
    private final TramoDescuentoRepository tramos;
    private final CatalogoMapper mapper;
    private final CacheCatalogo cache;
    private final CatalogoProperties propiedades;

    public DescuentoServiceImpl(ConfiguracionDescuentoRepository configuracion, TramoDescuentoRepository tramos,
            CatalogoMapper mapper, CacheCatalogo cache, CatalogoProperties propiedades) {
        this.propiedades = propiedades;
        this.configuracion = configuracion;
        this.tramos = tramos;
        this.mapper = mapper;
        this.cache = cache;
    }

    @Override
    public PoliticaDescuentosDto obtenerGlobal() {
        return cache.obtener(CacheCatalogo.DESCUENTOS_GLOBAL, new TypeReference<PoliticaDescuentosDto>() { },
                this::leerGlobal);
    }

    private PoliticaDescuentosDto leerGlobal() {
        boolean activo = configuracion.findById(1L).map(c -> c.isActivo()).orElse(false);
        List<TramoDto> lista = mapper.aTramosDto(tramos.findByProductoIdIsNullOrderByUnidadesMinimasAsc());
        return new PoliticaDescuentosDto(activo, lista);
    }

    @Override
    @Transactional
    public PoliticaDescuentosDto guardarGlobal(PoliticaDescuentosDto politica) {
        ValidadorTramos.validar(politica.tramos(), propiedades.descuentos().maxTramos(), propiedades.descuentos().porcentajeMax());
        tramos.deleteAll(tramos.findByProductoIdIsNullOrderByUnidadesMinimasAsc());
        tramos.flush();
        for (TramoDto t : politica.tramos()) {
            TramoDescuento nuevo = new TramoDescuento();
            nuevo.setUnidadesMinimas(t.unidadesMinimas());
            nuevo.setPorcentaje(t.porcentaje());
            tramos.save(nuevo);
        }
        ConfiguracionDescuento interruptor = configuracion.findById(1L).orElseGet(() -> {
            ConfiguracionDescuento nueva = new ConfiguracionDescuento();
            nueva.setId(1L);
            return nueva;
        });
        interruptor.setActivo(politica.activo());
        configuracion.saveAndFlush(interruptor);
        cache.invalidar(CacheCatalogo.DESCUENTOS_GLOBAL);
        cache.invalidarTodosLosProductos();
        return leerGlobal();
    }

    @Override
    @Transactional
    public void configurarProducto(Producto producto, DescuentoProductoRequest peticion) {
        List<TramoDto> propios = peticion.tramos() == null ? List.of() : peticion.tramos();
        if (peticion.modo() == ModoDescuento.PERSONALIZADO) {
            if (propios.isEmpty()) {
                throw new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION", "Agrega al menos un tramo de descuento");
            }
            ValidadorTramos.validar(propios, propiedades.descuentos().maxTramos(), propiedades.descuentos().porcentajeMax());
        }
        tramos.deleteAll(tramos.findByProductoIdOrderByUnidadesMinimasAsc(producto.getId()));
        tramos.flush();
        if (peticion.modo() == ModoDescuento.PERSONALIZADO) {
            for (TramoDto t : propios) {
                TramoDescuento nuevo = new TramoDescuento();
                nuevo.setProductoId(producto.getId());
                nuevo.setUnidadesMinimas(t.unidadesMinimas());
                nuevo.setPorcentaje(t.porcentaje());
                tramos.save(nuevo);
            }
        }
        producto.setModoDescuento(peticion.modo());
        cache.invalidarProducto(producto.getSlug());
    }

    @Override
    public PoliticaDescuentos politicaPara(Collection<Producto> productos) {
        PoliticaDescuentosDto global = obtenerGlobal();
        Map<Long, ModoDescuento> modos = new HashMap<>();
        for (Producto p : productos) {
            modos.put(p.getId(), p.getModoDescuento());
        }
        List<Long> personalizados = productos.stream()
                .filter(p -> p.getModoDescuento() == ModoDescuento.PERSONALIZADO).map(Producto::getId).toList();
        Map<Long, List<Tramo>> propios = personalizados.isEmpty() ? Map.of()
                : tramos.findByProductoIdIn(personalizados).stream().collect(Collectors.groupingBy(
                        TramoDescuento::getProductoId,
                        Collectors.mapping(t -> new Tramo(t.getUnidadesMinimas(), t.getPorcentaje()), Collectors.toList())));
        List<Tramo> tramosGlobales = global.tramos().stream()
                .map(t -> new Tramo(t.unidadesMinimas(), t.porcentaje())).toList();
        return new PoliticaDescuentos(global.activo(), tramosGlobales, modos, propios);
    }
}
