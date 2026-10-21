package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.ColorDto;
import com.fitstore.catalog.catalogo.dto.ColorRequest;
import com.fitstore.catalog.catalogo.dto.TallaDto;
import com.fitstore.catalog.catalogo.dto.TallaRequest;
import com.fitstore.catalog.catalogo.mapper.CatalogoMapper;
import com.fitstore.catalog.catalogo.model.Color;
import com.fitstore.catalog.catalogo.model.Talla;
import com.fitstore.catalog.catalogo.repository.ColorRepository;
import com.fitstore.catalog.catalogo.repository.TallaRepository;
import com.fitstore.catalog.catalogo.repository.VarianteRepository;
import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MaestrosServiceImpl implements MaestrosService {

    private final TallaRepository tallas;
    private final ColorRepository colores;
    private final VarianteRepository variantes;
    private final CatalogoMapper mapper;
    private final CacheCatalogo cache;

    public MaestrosServiceImpl(TallaRepository tallas, ColorRepository colores, VarianteRepository variantes,
            CatalogoMapper mapper, CacheCatalogo cache) {
        this.tallas = tallas;
        this.colores = colores;
        this.variantes = variantes;
        this.mapper = mapper;
        this.cache = cache;
    }

    // ---------------------------------------------------------------- tallas

    @Override
    @Transactional(readOnly = true)
    public List<TallaDto> listarTallas() {
        return mapper.aTallasDto(tallas.findAllByOrderByOrdenAsc());
    }

    @Override
    public TallaDto crearTalla(TallaRequest peticion) {
        String codigo = peticion.codigo().trim().toUpperCase();
        if (tallas.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictoNegocioException("TALLA_DUPLICADA", "Ya existe la talla " + codigo);
        }
        Talla talla = new Talla();
        talla.setCodigo(codigo);
        talla.setOrden(peticion.orden());
        talla.setActivo(peticion.activo() == null || peticion.activo());
        tallas.saveAndFlush(talla);
        cache.invalidarTodosLosProductos();
        return mapper.aDto(talla);
    }

    @Override
    public TallaDto editarTalla(Long id, TallaRequest peticion) {
        Talla talla = tallas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos la talla"));
        String codigo = peticion.codigo().trim().toUpperCase();
        Optional<Talla> otra = tallas.findByCodigoIgnoreCase(codigo);
        if (otra.isPresent() && !otra.get().getId().equals(id)) {
            throw new ConflictoNegocioException("TALLA_DUPLICADA", "Ya existe la talla " + codigo);
        }
        talla.setCodigo(codigo);
        talla.setOrden(peticion.orden());
        if (peticion.activo() != null) {
            talla.setActivo(peticion.activo());
        }
        tallas.saveAndFlush(talla);
        cache.invalidarTodosLosProductos();
        return mapper.aDto(talla);
    }

    @Override
    public void eliminarTalla(Long id) {
        Talla talla = tallas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos la talla"));
        if (variantes.existsByTallaId(id)) {
            throw new ConflictoNegocioException("TALLA_EN_USO", "La talla " + talla.getCodigo() + " la usan variantes; desactívala");
        }
        tallas.delete(talla);
        tallas.flush();
    }

    // ---------------------------------------------------------------- colores

    @Override
    @Transactional(readOnly = true)
    public List<ColorDto> listarColores() {
        return mapper.aColoresDto(colores.findAllByOrderByIdAsc());
    }

    @Override
    public ColorDto crearColor(ColorRequest peticion) {
        String nombre = peticion.nombre().trim();
        if (colores.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new ConflictoNegocioException("COLOR_DUPLICADO", "Ya existe el color " + nombre);
        }
        Color color = new Color();
        color.setNombre(nombre);
        color.setHex(peticion.hex().toUpperCase());
        color.setActivo(peticion.activo() == null || peticion.activo());
        colores.saveAndFlush(color);
        cache.invalidarTodosLosProductos();
        return mapper.aDto(color);
    }

    @Override
    public ColorDto editarColor(Long id, ColorRequest peticion) {
        Color color = colores.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos el color"));
        String nombre = peticion.nombre().trim();
        Optional<Color> otro = colores.findByNombreIgnoreCase(nombre);
        if (otro.isPresent() && !otro.get().getId().equals(id)) {
            throw new ConflictoNegocioException("COLOR_DUPLICADO", "Ya existe el color " + nombre);
        }
        color.setNombre(nombre);
        color.setHex(peticion.hex().toUpperCase());
        if (peticion.activo() != null) {
            color.setActivo(peticion.activo());
        }
        colores.saveAndFlush(color);
        cache.invalidarTodosLosProductos();
        return mapper.aDto(color);
    }

    @Override
    public void eliminarColor(Long id) {
        Color color = colores.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos el color"));
        if (variantes.existsByColorId(id)) {
            throw new ConflictoNegocioException("COLOR_EN_USO", "El color " + color.getNombre() + " lo usan variantes; desactívalo");
        }
        colores.delete(color);
        colores.flush();
    }
}
