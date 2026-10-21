package com.fitstore.settings.reclamaciones.mapper;

import com.fitstore.settings.reclamaciones.dto.BienContratadoRequest;
import com.fitstore.settings.reclamaciones.dto.ConsumidorRequest;
import com.fitstore.settings.reclamaciones.dto.ReclamoDto;
import com.fitstore.settings.reclamaciones.model.BienContratado;
import com.fitstore.settings.reclamaciones.model.Consumidor;
import org.mapstruct.Mapper;

/** Traduce la Hoja de Reclamación entre la solicitud, la entidad y el detalle que ve el staff. */
@Mapper(componentModel = "spring")
public interface ReclamoMapper {

    Consumidor aEntidad(ConsumidorRequest solicitud);

    BienContratado aEntidad(BienContratadoRequest solicitud);

    ReclamoDto.ConsumidorDto aDto(Consumidor consumidor);

    ReclamoDto.BienDto aDto(BienContratado bien);
}
