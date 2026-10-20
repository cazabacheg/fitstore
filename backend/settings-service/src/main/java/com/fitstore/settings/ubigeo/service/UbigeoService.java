package com.fitstore.settings.ubigeo.service;

import com.fitstore.settings.ubigeo.dto.DistritoEncontradoDto;
import com.fitstore.settings.ubigeo.dto.UbigeoItemDto;
import com.fitstore.settings.ubigeo.dto.UbigeoNombreDto;
import java.util.List;
import java.util.Optional;

/** Consultas del maestro de ubigeo (INEI), solo lectura (RF-SET-08). */
public interface UbigeoService {

    List<UbigeoItemDto> departamentos();

    /** @throws com.fitstore.common.api.RecursoNoEncontradoException si el departamento no existe */
    List<UbigeoItemDto> provincias(String departamentoCodigo);

    /** @throws com.fitstore.common.api.RecursoNoEncontradoException si la provincia no existe */
    List<UbigeoItemDto> distritos(String provinciaCodigo);

    /** Hasta 20 distritos cuyo nombre contiene el texto, sin importar tildes ni mayúsculas. */
    List<DistritoEncontradoDto> buscarDistritos(String texto);

    /** Nombre de un código de 2, 4 o 6 dígitos. @throws com.fitstore.common.api.RecursoNoEncontradoException si no existe */
    UbigeoNombreDto nombre(String codigo);

    /** "Miraflores, Lima, Lima" para un distrito, o vacío si el código no existe. */
    Optional<String> nombreCompletoDeDistrito(String codigo);
}
