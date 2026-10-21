package com.fitstore.delivery.repository;

import com.fitstore.delivery.model.Entrega;
import com.fitstore.delivery.model.EstadoEntrega;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;

/** Implementación de {@link EntregaBusqueda}: un solo {@code GROUP BY estado}. */
public class EntregaBusquedaImpl implements EntregaBusqueda {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Map<EstadoEntrega, Long> contarPorEstado(Specification<Entrega> filtro) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> consulta = cb.createQuery(Object[].class);
        Root<Entrega> raiz = consulta.from(Entrega.class);
        Predicate condicion = filtro.toPredicate(raiz, consulta, cb);
        if (condicion != null) {
            consulta.where(condicion);
        }
        consulta.multiselect(raiz.get("estado"), cb.count(raiz)).groupBy(raiz.get("estado"));
        Map<EstadoEntrega, Long> cuentas = new EnumMap<>(EstadoEntrega.class);
        for (Object[] fila : em.createQuery(consulta).getResultList()) {
            cuentas.put((EstadoEntrega) fila[0], (Long) fila[1]);
        }
        return cuentas;
    }
}
