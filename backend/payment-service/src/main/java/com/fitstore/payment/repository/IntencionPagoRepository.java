package com.fitstore.payment.repository;

import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.IntencionPago;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntencionPagoRepository extends JpaRepository<IntencionPago, UUID> {

    Optional<IntencionPago> findByPedidoId(UUID pedidoId);

    Optional<IntencionPago> findByCodigoPedido(String codigoPedido);

    List<IntencionPago> findByClienteIdOrderByCreadoEnDesc(UUID clienteId);

    Page<IntencionPago> findByEstado(EstadoIntencion estado, Pageable paginacion);

    List<IntencionPago> findByCreadoEnBetween(Instant desde, Instant hasta);
}
