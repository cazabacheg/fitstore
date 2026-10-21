package com.fitstore.catalog.inventario.repository;

import java.util.Collection;
import com.fitstore.catalog.inventario.model.ReservaItem;
import com.fitstore.catalog.inventario.model.ReservaItemId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaItemRepository extends JpaRepository<ReservaItem, ReservaItemId> {

    List<ReservaItem> findByPedidoId(UUID pedidoId);

    boolean existsByVarianteIdIn(Collection<Long> varianteIds);
}
