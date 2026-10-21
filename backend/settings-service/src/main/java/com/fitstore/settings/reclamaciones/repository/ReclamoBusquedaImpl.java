package com.fitstore.settings.reclamaciones.repository;

import com.fitstore.settings.config.SettingsConfig;
import com.fitstore.settings.reclamaciones.dto.FiltroReclamos;
import com.fitstore.settings.reclamaciones.model.EstadoReclamo;
import com.fitstore.settings.reclamaciones.model.Reclamo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Implementación con Criteria API: los filtros nulos no se aplican y el orden solo admite columnas conocidas. */
public class ReclamoBusquedaImpl implements ReclamoBusqueda {

    private static final Set<String> ORDENABLES = Set.of("creadoEn", "fechaLimite", "numero", "estado", "tipo");

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Reclamo> buscar(FiltroReclamos filtro, LocalDate hoy, LocalDate limitePorVencer, Pageable paginacion) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Reclamo> consulta = cb.createQuery(Reclamo.class);
        Root<Reclamo> raiz = consulta.from(Reclamo.class);
        consulta.select(raiz).where(condiciones(cb, raiz, filtro, hoy, limitePorVencer).toArray(new Predicate[0]))
                .orderBy(orden(cb, raiz, paginacion.getSort()));
        List<Reclamo> contenido = em.createQuery(consulta).setFirstResult((int) paginacion.getOffset())
                .setMaxResults(paginacion.getPageSize()).getResultList();

        CriteriaQuery<Long> conteo = cb.createQuery(Long.class);
        Root<Reclamo> raizConteo = conteo.from(Reclamo.class);
        conteo.select(cb.count(raizConteo))
                .where(condiciones(cb, raizConteo, filtro, hoy, limitePorVencer).toArray(new Predicate[0]));
        long total = em.createQuery(conteo).getSingleResult();

        return new PageImpl<>(contenido, paginacion, total);
    }

    private List<Predicate> condiciones(CriteriaBuilder cb, Root<Reclamo> r, FiltroReclamos f, LocalDate hoy,
            LocalDate limitePorVencer) {
        List<Predicate> condiciones = new ArrayList<>();
        if (f.estado() != null) {
            condiciones.add(cb.equal(r.get("estado"), f.estado()));
        }
        if (f.tipo() != null) {
            condiciones.add(cb.equal(r.get("tipo"), f.tipo()));
        }
        if (f.q() != null && !f.q().isBlank()) {
            String patron = "%" + escapar(f.q().trim().toLowerCase(Locale.ROOT)) + "%";
            condiciones.add(cb.or(
                    cb.like(cb.lower(r.get("numero")), patron, '\\'),
                    cb.like(cb.lower(cb.concat(cb.concat(r.get("consumidor").get("nombres"), " "),
                            r.get("consumidor").get("apellidos"))), patron, '\\'),
                    cb.like(cb.lower(r.get("consumidor").get("numeroDocumento")), patron, '\\')));
        }
        if (f.desde() != null) {
            condiciones.add(cb.greaterThanOrEqualTo(r.get("creadoEn"),
                    f.desde().atStartOfDay(SettingsConfig.ZONA_LIMA).toInstant()));
        }
        if (f.hasta() != null) {
            condiciones.add(cb.lessThan(r.get("creadoEn"),
                    f.hasta().plusDays(1).atStartOfDay(SettingsConfig.ZONA_LIMA).toInstant()));
        }
        Predicate sinResponder = cb.notEqual(r.get("estado"), EstadoReclamo.RESPONDIDO);
        if (f.vencidos()) {
            condiciones.add(cb.and(sinResponder, cb.lessThan(r.<LocalDate>get("fechaLimite"), hoy)));
        }
        if (f.porVencer()) {
            condiciones.add(cb.and(sinResponder, cb.between(r.<LocalDate>get("fechaLimite"), hoy, limitePorVencer)));
        }
        return condiciones;
    }

    private List<Order> orden(CriteriaBuilder cb, Root<Reclamo> r, Sort sort) {
        List<Order> orden = new ArrayList<>();
        for (Sort.Order o : sort) {
            if (ORDENABLES.contains(o.getProperty())) {
                orden.add(o.isAscending() ? cb.asc(r.get(o.getProperty())) : cb.desc(r.get(o.getProperty())));
            }
        }
        if (orden.isEmpty()) {
            orden.add(cb.desc(r.get("creadoEn")));
        }
        orden.add(cb.desc(r.get("id")));
        return orden;
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
