package com.fitstore.order.pedidos.repository;

import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.Pedido;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Filtros del listado del backoffice (micro order 4.1): estado, modalidad, fechas, código y cliente. */
public final class PedidoSpecifications {

    private PedidoSpecifications() {
    }

    /** Combina los filtros que no son nulos; con todos nulos devuelve todos los pedidos. */
    public static Specification<Pedido> filtrar(EstadoPedido estado, Modalidad modalidad, Instant desde, Instant hasta,
            String codigo, UUID clienteId) {
        Specification<Pedido> filtro = (raiz, consulta, cb) -> cb.conjunction();
        if (estado != null) {
            filtro = filtro.and((raiz, consulta, cb) -> cb.equal(raiz.get("estado"), estado));
        }
        if (modalidad != null) {
            filtro = filtro.and((raiz, consulta, cb) -> cb.equal(raiz.get("modalidad"), modalidad));
        }
        if (desde != null) {
            filtro = filtro.and((raiz, consulta, cb) -> cb.greaterThanOrEqualTo(raiz.get("creadoEn"), desde));
        }
        if (hasta != null) {
            filtro = filtro.and((raiz, consulta, cb) -> cb.lessThan(raiz.get("creadoEn"), hasta));
        }
        if (codigo != null && !codigo.isBlank()) {
            String patron = "%" + codigo.trim().toLowerCase() + "%";
            filtro = filtro.and((raiz, consulta, cb) -> cb.like(cb.lower(raiz.get("codigo")), patron));
        }
        if (clienteId != null) {
            filtro = filtro.and((raiz, consulta, cb) -> cb.equal(raiz.get("clienteId"), clienteId));
        }
        return filtro;
    }
}
