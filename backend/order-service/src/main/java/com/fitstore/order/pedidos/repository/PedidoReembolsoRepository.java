package com.fitstore.order.pedidos.repository;

import com.fitstore.order.pedidos.model.PedidoReembolso;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Reembolsos postventa de los pedidos (micro order 3.1 y 4.8). */
public interface PedidoReembolsoRepository extends JpaRepository<PedidoReembolso, UUID> {

    List<PedidoReembolso> findByPedidoIdOrderByCreadoEnAsc(UUID pedidoId);
}
