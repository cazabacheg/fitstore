package com.fitstore.auth.repository;

import com.fitstore.auth.model.EstadoStaff;
import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Filtros del listado de staff: texto (nombre o correo), rol y estado deducido. */
public final class StaffSpecs {

    private StaffSpecs() {
    }

    public static Specification<Staff> filtrar(String texto, RolStaff rol, EstadoStaff estado) {
        return (raiz, consulta, cb) -> {
            Predicate todo = cb.conjunction();
            if (texto != null && !texto.isBlank()) {
                String patron = "%" + escapar(texto.trim().toLowerCase(Locale.ROOT)) + "%";
                Expression<String> nombre = cb.lower(cb.concat(cb.concat(raiz.<String>get("nombres"), " "), raiz.<String>get("apellidos")));
                todo = cb.and(todo, cb.or(cb.like(nombre, patron, '\\'), cb.like(cb.lower(raiz.<String>get("email")), patron, '\\')));
            }
            if (rol != null) {
                todo = cb.and(todo, cb.equal(raiz.get("rol"), rol));
            }
            if (estado != null) {
                todo = cb.and(todo, switch (estado) {
                    case INACTIVO -> cb.isFalse(raiz.get("activo"));
                    case ACTIVO -> cb.and(cb.isTrue(raiz.get("activo")), cb.isNotNull(raiz.get("passwordHash")));
                    case INVITACION_PENDIENTE -> cb.and(cb.isTrue(raiz.get("activo")), cb.isNull(raiz.get("passwordHash")));
                });
            }
            return todo;
        };
    }

    /** Los comodines de LIKE escritos por el usuario se toman literalmente. */
    static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
