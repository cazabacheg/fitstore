package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.ColorDto;
import com.fitstore.catalog.catalogo.dto.ColorRequest;
import com.fitstore.catalog.catalogo.dto.TallaDto;
import com.fitstore.catalog.catalogo.dto.TallaRequest;
import java.util.List;

/** Tallas y colores maestros: listas únicas administradas por el ADMIN (D1, HU-2-07). */
public interface MaestrosService {

    List<TallaDto> listarTallas();

    TallaDto crearTalla(TallaRequest peticion);

    TallaDto editarTalla(Long id, TallaRequest peticion);

    /** Solo si ninguna variante la usa; si no, 409 (CA-2-34). */
    void eliminarTalla(Long id);

    List<ColorDto> listarColores();

    ColorDto crearColor(ColorRequest peticion);

    ColorDto editarColor(Long id, ColorRequest peticion);

    /** Solo si ninguna variante lo usa; si no, 409 (CA-2-34). */
    void eliminarColor(Long id);
}
