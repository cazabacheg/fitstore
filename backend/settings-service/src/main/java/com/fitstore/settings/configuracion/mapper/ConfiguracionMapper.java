package com.fitstore.settings.configuracion.mapper;

import com.fitstore.settings.configuracion.dto.BeneficioDto;
import com.fitstore.settings.configuracion.dto.ContenidoRequest;
import com.fitstore.settings.configuracion.dto.IdentidadRequest;
import com.fitstore.settings.configuracion.dto.ParametrosRequest;
import com.fitstore.settings.configuracion.dto.TemaDto;
import com.fitstore.settings.configuracion.dto.TemaRequest;
import com.fitstore.settings.configuracion.model.Beneficio;
import com.fitstore.settings.configuracion.model.Contenido;
import com.fitstore.settings.configuracion.model.Identidad;
import com.fitstore.settings.configuracion.model.Parametros;
import com.fitstore.settings.configuracion.model.Tema;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** Traduce entre las solicitudes, las entidades y los DTO de la configuración. */
@Mapper(componentModel = "spring")
public interface ConfiguracionMapper {

    void actualizar(IdentidadRequest solicitud, @MappingTarget Identidad identidad);

    void actualizar(TemaRequest solicitud, @MappingTarget Tema tema);

    void actualizar(ParametrosRequest solicitud, @MappingTarget Parametros parametros);

    /** La tienda física se actualiza aparte porque valida el distrito. */
    @Mapping(target = "tiendaDireccion", ignore = true)
    @Mapping(target = "tiendaUbigeo", ignore = true)
    @Mapping(target = "tiendaReferencia", ignore = true)
    void actualizar(ContenidoRequest solicitud, @MappingTarget Contenido contenido);

    TemaDto aDto(Tema tema);

    BeneficioDto aDto(Beneficio beneficio);

    Beneficio aModelo(BeneficioDto dto);

    List<BeneficioDto> aDtos(List<Beneficio> beneficios);
}
