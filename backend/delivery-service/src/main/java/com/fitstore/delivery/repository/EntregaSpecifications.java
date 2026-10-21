package com.fitstore.delivery.repository;

import com.fitstore.delivery.dto.FiltroEntregas;
import com.fitstore.delivery.model.Entrega;
import com.fitstore.delivery.model.EstadoEntrega;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Filtros del listado de entregas (CA-6-31 y CA-6-32): todos opcionales y combinados con Y. */
public final class EntregaSpecifications {

    private EntregaSpecifications() {
    }

    /**
     * @param incluirEstado {@code false} para los contadores de las pestañas, que cuentan por estado sin aplicar el
     *                      filtro de estado pero sí los demás
     */
    public static Specification<Entrega> filtrar(FiltroEntregas filtro, boolean incluirEstado) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.modalidad() != null) {
                condiciones.add(cb.equal(root.get("modalidad"), filtro.modalidad()));
            }
            if (incluirEstado && filtro.estado() != null) {
                condiciones.add(cb.equal(root.get("estado"), filtro.estado()));
            }
            if (filtro.fecha() != null) {
                condiciones.add(cb.equal(root.get("fechaProgramada"), filtro.fecha()));
            }
            if (filtro.repartidorId() != null) {
                condiciones.add(cb.equal(root.get("repartidor").get("id"), filtro.repartidorId()));
            }
            if (filtro.codigo() != null && !filtro.codigo().isBlank()) {
                String patron = "%" + filtro.codigo().trim().toLowerCase().replace("%", "").replace("_", "") + "%";
                condiciones.add(cb.like(cb.lower(root.get("codigoPedido")), patron));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    /** Las pendientes de asignar o de enviar primero, y luego las más antiguas (se aplica solo si no se pide un orden). */
    public static Specification<Entrega> pendientesPrimero() {
        return (root, query, cb) -> {
            if (Entrega.class.equals(query.getResultType())) {
                Expression<Integer> prioridad = cb.<Integer>selectCase()
                        .when(root.get("estado").in(EstadoEntrega.PENDIENTE_ASIGNACION, EstadoEntrega.PENDIENTE_ENVIO), 0)
                        .otherwise(1);
                query.orderBy(cb.asc(prioridad), cb.asc(root.get("creadoEn")));
            }
            return null;
        };
    }
}
