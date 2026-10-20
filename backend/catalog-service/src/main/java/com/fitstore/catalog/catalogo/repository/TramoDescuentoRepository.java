package com.fitstore.catalog.catalogo.repository;

import com.fitstore.catalog.catalogo.model.TramoDescuento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TramoDescuentoRepository extends JpaRepository<TramoDescuento, Long> {

    /** Política global (sin producto), de menor a mayor número de unidades. */
    List<TramoDescuento> findByProductoIdIsNullOrderByUnidadesMinimasAsc();

    List<TramoDescuento> findByProductoIdOrderByUnidadesMinimasAsc(Long productoId);
}
