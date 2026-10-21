package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.CategoriaAdminDto;
import com.fitstore.catalog.catalogo.dto.CategoriaRequest;
import com.fitstore.catalog.catalogo.dto.GuiaTallasDto;
import java.util.List;

/** Administración de categorías (árbol de 2 niveles) y de su guía de tallas (HU-2-04). */
public interface CategoriaService {

    /** Todas las categorías, activas e inactivas, con su conteo de productos. */
    List<CategoriaAdminDto> listar();

    CategoriaAdminDto crear(CategoriaRequest peticion);

    CategoriaAdminDto editar(Long id, CategoriaRequest peticion);

    /** Borrado físico; con hijas o con productos responde 409 y se sugiere desactivarla (CA-2-19). */
    void eliminar(Long id);

    CategoriaAdminDto desactivar(Long id);

    CategoriaAdminDto activar(Long id);

    /** Mueve la categoría a la posición {@code orden} entre sus hermanas y devuelve el árbol completo (CA-2-18). */
    List<CategoriaAdminDto> ordenar(Long id, int orden);

    GuiaTallasDto guardarGuiaTallas(Long categoriaId, GuiaTallasDto guia);
}
