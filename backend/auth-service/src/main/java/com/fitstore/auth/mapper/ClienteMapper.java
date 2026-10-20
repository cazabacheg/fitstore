package com.fitstore.auth.mapper;

import com.fitstore.auth.dto.request.PerfilClienteRequest;
import com.fitstore.auth.dto.response.ClienteResponse;
import com.fitstore.auth.model.Cliente;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    ClienteResponse aPerfil(Cliente cliente);

    /** Aplica los campos editables del perfil; el correo y el DNI no se tocan. */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "nombres", source = "nombres")
    @Mapping(target = "apellidos", source = "apellidos")
    @Mapping(target = "telefono", source = "telefono")
    void actualizar(PerfilClienteRequest solicitud, @MappingTarget Cliente cliente);
}
