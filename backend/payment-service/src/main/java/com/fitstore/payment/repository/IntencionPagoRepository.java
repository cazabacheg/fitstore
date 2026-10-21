package com.fitstore.payment.repository;

import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.IntencionPago;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntencionPagoRepository
        extends JpaRepository<IntencionPago, UUID>, JpaSpecificationExecutor<IntencionPago> {

    /** Bloquea la intención ({@code SELECT … FOR UPDATE}) mientras dura la transacción: base del cobro sin doble pago. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from IntencionPago i where i.codigoPedido = :codigo")
    Optional<IntencionPago> bloquearPorCodigoPedido(@Param("codigo") String codigoPedido);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from IntencionPago i where i.id = :id")
    Optional<IntencionPago> bloquearPorId(@Param("id") UUID id);

    /** Intenciones cobradas que todavía no tienen boleta (las toma el proceso de boletas). */
    @Query("""
            select i from IntencionPago i
            where i.estado in (com.fitstore.payment.model.EstadoIntencion.PAGADA,
                               com.fitstore.payment.model.EstadoIntencion.REEMBOLSADA_PARCIAL,
                               com.fitstore.payment.model.EstadoIntencion.REEMBOLSADA)
              and not exists (select 1 from Boleta b where b.intencionId = i.id)
            order by i.actualizadoEn""")
    List<IntencionPago> sinBoleta();

    Optional<IntencionPago> findByPedidoId(UUID pedidoId);

    Optional<IntencionPago> findByCodigoPedido(String codigoPedido);

    List<IntencionPago> findByClienteIdOrderByCreadoEnDesc(UUID clienteId);

    Page<IntencionPago> findByEstado(EstadoIntencion estado, Pageable paginacion);

    List<IntencionPago> findByCreadoEnBetween(Instant desde, Instant hasta);
}
