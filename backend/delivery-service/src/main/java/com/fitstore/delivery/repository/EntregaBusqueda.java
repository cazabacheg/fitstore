package com.fitstore.delivery.repository;

import com.fitstore.delivery.model.Entrega;
import com.fitstore.delivery.model.EstadoEntrega;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;

/** Consultas del listado que Spring Data no deriva: los contadores por estado. */
public interface EntregaBusqueda {

    /** Cuántas entregas hay por estado entre las que cumplen el filtro; los estados sin entregas no aparecen. */
    Map<EstadoEntrega, Long> contarPorEstado(Specification<Entrega> filtro);
}
