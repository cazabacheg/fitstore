package com.fitstore.auth.mapper;

import com.fitstore.auth.dto.request.PerfilStaffRequest;
import com.fitstore.auth.dto.response.StaffResponse;
import com.fitstore.auth.model.Staff;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StaffMapper {

    @Mapping(target = "estado", expression = "java(staff.estado())")
    StaffResponse aRespuesta(Staff staff);

    /** Aplica el nombre editable del propio perfil; el correo y el rol no se tocan. */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "nombres", source = "nombres")
    @Mapping(target = "apellidos", source = "apellidos")
    void actualizar(PerfilStaffRequest solicitud, @MappingTarget Staff staff);
}
