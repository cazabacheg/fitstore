package com.fitstore.auth.mapper;

import com.fitstore.auth.dto.request.DireccionRequest;
import com.fitstore.auth.dto.response.DireccionResponse;
import com.fitstore.auth.model.Direccion;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DireccionMapper {

    DireccionResponse aRespuesta(Direccion direccion);

    /** Copia los datos editables; el dueño y la condición de predeterminada los decide el servicio. */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "alias", source = "alias")
    @Mapping(target = "ubigeo", source = "ubigeo")
    @Mapping(target = "direccion", source = "direccion")
    @Mapping(target = "referencia", source = "referencia")
    @Mapping(target = "telefonoContacto", source = "telefonoContacto")
    void actualizar(DireccionRequest solicitud, @MappingTarget Direccion direccion);
}
