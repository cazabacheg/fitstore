package com.fitstore.order.pedidos.repository;

import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Pedido;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Pedidos (micro order 3.1). No hay borrado: los pedidos son registros comerciales (invariante 7). */
public interface PedidoRepository extends JpaRepository<Pedido, UUID>, JpaSpecificationExecutor<Pedido> {

    Optional<Pedido> findByCodigo(String codigo);

    /** Clave de idempotencia del checkout: si ya existe, se devuelve el mismo pedido (D5). */
    Optional<Pedido> findByClienteIdAndIdempotencyKey(UUID clienteId, String idempotencyKey);

    Page<Pedido> findByClienteId(UUID clienteId, Pageable pagina);

    /** Pedidos pendientes de pago cuyo plazo ya venció: los recorre el planificador de expiración. */
    List<Pedido> findByEstadoAndExpiraEnBefore(EstadoPedido estado, Instant limite);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Pedido p
               set p.estado = :hacia, p.actualizadoEn = :ahora, p.version = p.version + 1
             where p.id = :id and p.estado = :desde
            """)
    int actualizarEstadoSi(@Param("id") UUID id, @Param("desde") EstadoPedido desde,
            @Param("hacia") EstadoPedido hacia, @Param("ahora") Instant ahora);

    /**
     * Transición condicional (micro 4.4 y 10.2): {@code UPDATE ... WHERE id = ? AND estado = desde}. Devuelve 0 si
     * otra transición ganó la carrera (por ejemplo, el pago contra la expiración).
     */
    default int cambiarEstadoSi(UUID id, EstadoPedido desde, EstadoPedido hacia) {
        return actualizarEstadoSi(id, desde, hacia, Instant.now());
    }

    /** Siguiente número del código legible del pedido ({@code pedido_codigo_seq}, D4). */
    @Query(value = "select nextval('pedido_codigo_seq')", nativeQuery = true)
    long siguienteNumeroCodigo();
}
