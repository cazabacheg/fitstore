package com.fitstore.order.pedidos.repository;

import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.Pedido;
import jakarta.persistence.criteria.Expression;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros del listado del backoffice (micro order 4.1): estado, modalidad, fechas, código y cliente (nombre, correo
 * o DNI), y el orden "lo que hay que preparar primero".
 */
public final class PedidoSpecifications {

    private PedidoSpecifications() {
    }

    /** Combina los filtros que no son nulos; con todos nulos devuelve todos los pedidos. */
    public static Specification<Pedido> filtrar(EstadoPedido estado, Modalidad modalidad, Instant desde, Instant hasta,
            String codigo, UUID clienteId) {
        return filtrar(estado, modalidad, desde, hasta, codigo, null, clienteId);
    }

    /**
     * Igual que la anterior, más {@code cliente}: texto que se busca en el nombre del destinatario, su correo o su DNI
     * (sin distinguir mayúsculas).
     */
    public static Specification<Pedido> filtrar(EstadoPedido estado, Modalidad modalidad, Instant desde, Instant hasta,
            String codigo, String cliente, UUID clienteId) {
        Specification<Pedido> filtro = (raiz, consulta, cb) -> cb.conjunction();
        if (estado != null) {
            filtro = filtro.and(conEstado(estado));
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
        if (cliente != null && !cliente.isBlank()) {
            String patron = "%" + cliente.trim().toLowerCase() + "%";
            filtro = filtro.and((raiz, consulta, cb) -> {
                var destinatario = raiz.get("destinatario");
                Expression<String> nombreCompleto = cb.concat(cb.concat(destinatario.<String>get("nombres"), " "),
                        destinatario.<String>get("apellidos"));
                return cb.or(cb.like(cb.lower(nombreCompleto), patron),
                        cb.like(cb.lower(destinatario.<String>get("email")), patron),
                        cb.like(destinatario.<String>get("dni"), patron));
            });
        }
        if (clienteId != null) {
            filtro = filtro.and((raiz, consulta, cb) -> cb.equal(raiz.get("clienteId"), clienteId));
        }
        return filtro;
    }

    public static Specification<Pedido> conEstado(EstadoPedido estado) {
        return (raiz, consulta, cb) -> cb.equal(raiz.get("estado"), estado);
    }

    /**
     * Orden por defecto del backoffice (CA-4-40): primero lo que espera al operador (PAGADO, EN_PREPARACION, luego los
     * listos), y dentro de cada grupo los más antiguos. Solo ordena la consulta de datos, no la de conteo.
     */
    public static Specification<Pedido> prioridadDePreparacion() {
        return (raiz, consulta, cb) -> {
            if (Pedido.class.equals(consulta.getResultType())) {
                var estado = raiz.<EstadoPedido>get("estado");
                Expression<Integer> prioridad = cb.<Integer>selectCase()
                        .when(cb.equal(estado, EstadoPedido.PAGADO), 0)
                        .when(cb.equal(estado, EstadoPedido.EN_PREPARACION), 1)
                        .when(estado.in(EstadoPedido.LISTO_DESPACHO, EstadoPedido.LISTO_RECOJO), 2)
                        .otherwise(3);
                consulta.orderBy(cb.asc(prioridad), cb.asc(raiz.get("creadoEn")));
            }
            return cb.conjunction();
        };
    }
}
