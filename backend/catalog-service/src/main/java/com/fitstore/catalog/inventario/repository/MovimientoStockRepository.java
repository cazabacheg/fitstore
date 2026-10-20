package com.fitstore.catalog.inventario.repository;

import com.fitstore.catalog.inventario.model.MovimientoStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    /** Kardex de una variante, del más reciente al más antiguo. */
    Page<MovimientoStock> findByVarianteIdOrderByCreadoEnDesc(Long varianteId, Pageable pageable);
}
