package com.fitstore.settings.ubigeo.repository;

import com.fitstore.settings.ubigeo.model.Departamento;
import com.fitstore.settings.ubigeo.model.Distrito;
import com.fitstore.settings.ubigeo.model.Provincia;
import java.util.List;
import java.util.Optional;

/** Fachada de solo lectura sobre el maestro de ubigeo (departamento, provincia y distrito). */
public interface UbigeoRepository {

    List<Departamento> departamentos();

    List<Provincia> provinciasDe(String departamentoCodigo);

    List<Distrito> distritosDe(String provinciaCodigo);

    List<Distrito> buscarDistritos(String texto);

    /** "Miraflores, Lima, Lima" (distrito, provincia, departamento) para un código de 6 dígitos. */
    Optional<String> nombreCompleto(String distritoCodigo);
}
