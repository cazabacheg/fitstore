package com.fitstore.catalog.inventario.repository;

import java.util.Collection;
import com.fitstore.catalog.inventario.model.MovimientoStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    /** Kardex de una variante, del más reciente al más antiguo. */
    Page<MovimientoStock> findByVarianteIdOrderByCreadoEnDesc(Long varianteId, Pageable pageable);

    boolean existsByVarianteIdIn(Collection<Long> varianteIds);

    /** Kardex con desempate por id: movimientos del mismo instante salen del último al primero. */
    Page<MovimientoStock> findByVarianteIdOrderByCreadoEnDescIdDesc(Long varianteId, Pageable pageable);
}
