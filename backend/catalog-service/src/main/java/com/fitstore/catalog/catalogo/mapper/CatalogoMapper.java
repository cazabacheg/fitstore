package com.fitstore.catalog.catalogo.mapper;

import com.fitstore.catalog.catalogo.dto.CategoriaHijaDto;
import com.fitstore.catalog.catalogo.dto.ColorDto;
import com.fitstore.catalog.catalogo.dto.TallaDto;
import com.fitstore.catalog.catalogo.dto.TramoDto;
import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.Color;
import com.fitstore.catalog.catalogo.model.Talla;
import com.fitstore.catalog.catalogo.model.TramoDescuento;
import com.fitstore.catalog.catalogo.service.dominio.Tramo;
import java.util.List;
import org.mapstruct.Mapper;

/** Mapeo de las entidades simples del catálogo a sus DTO (ADR-015). */
@Mapper(componentModel = "spring")
public interface CatalogoMapper {

    TallaDto aDto(Talla talla);

    ColorDto aDto(Color color);

    CategoriaHijaDto aHijaDto(Categoria categoria);

    TramoDto aDto(TramoDescuento tramo);

    TramoDto aDto(Tramo tramo);

    List<TallaDto> aTallasDto(List<Talla> tallas);

    List<ColorDto> aColoresDto(List<Color> colores);

    List<TramoDto> aTramosDto(List<TramoDescuento> tramos);

    default Tramo aTramo(TramoDto dto) {
        return new Tramo(dto.unidadesMinimas(), dto.porcentaje());
    }
}
