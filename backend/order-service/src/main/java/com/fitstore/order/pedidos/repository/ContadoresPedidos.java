package com.fitstore.order.pedidos.repository;

import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Pedido;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/** Cuántos pedidos hay en cada estado para los chips del listado del backoffice (CA-4-40). */
@Repository
public class ContadoresPedidos {

    private final EntityManager em;

    public ContadoresPedidos(EntityManager em) {
        this.em = em;
    }

    /** Pedidos por estado que cumplen el filtro; todos los estados aparecen, con 0 si no hay ninguno. */
    public Map<EstadoPedido, Long> porEstado(Specification<Pedido> filtro) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> consulta = cb.createTupleQuery();
        Root<Pedido> raiz = consulta.from(Pedido.class);
        consulta.multiselect(raiz.get("estado").alias("estado"), cb.count(raiz).alias("total"));
        Predicate condicion = filtro.toPredicate(raiz, consulta, cb);
        if (condicion != null) {
            consulta.where(condicion);
        }
        consulta.groupBy(raiz.get("estado"));
        Map<EstadoPedido, Long> contadores = new EnumMap<>(EstadoPedido.class);
        for (EstadoPedido estado : EstadoPedido.values()) {
            contadores.put(estado, 0L);
        }
        for (Tuple fila : em.createQuery(consulta).getResultList()) {
            contadores.put(fila.get("estado", EstadoPedido.class), fila.get("total", Long.class));
        }
        return contadores;
    }
}
