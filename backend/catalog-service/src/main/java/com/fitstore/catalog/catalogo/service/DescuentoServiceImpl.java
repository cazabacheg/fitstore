package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.PoliticaDescuentosDto;
import com.fitstore.catalog.catalogo.dto.TramoDto;
import com.fitstore.catalog.catalogo.mapper.CatalogoMapper;
import com.fitstore.catalog.catalogo.model.ModoDescuento;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.TramoDescuento;
import com.fitstore.catalog.catalogo.repository.ConfiguracionDescuentoRepository;
import com.fitstore.catalog.catalogo.repository.TramoDescuentoRepository;
import com.fitstore.catalog.catalogo.service.dominio.PoliticaDescuentos;
import com.fitstore.catalog.catalogo.service.dominio.Tramo;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

@Service
public class DescuentoServiceImpl implements DescuentoService {

    private final ConfiguracionDescuentoRepository configuracion;
    private final TramoDescuentoRepository tramos;
    private final CatalogoMapper mapper;
    private final CacheCatalogo cache;

    public DescuentoServiceImpl(ConfiguracionDescuentoRepository configuracion, TramoDescuentoRepository tramos,
            CatalogoMapper mapper, CacheCatalogo cache) {
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
