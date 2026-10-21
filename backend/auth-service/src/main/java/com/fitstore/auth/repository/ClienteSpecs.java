package com.fitstore.auth.repository;

import com.fitstore.auth.model.Cliente;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Filtros del listado de clientes: texto (nombre, correo o DNI) y estado activo. */
public final class ClienteSpecs {

    private ClienteSpecs() {
    }

    public static Specification<Cliente> filtrar(String texto, Boolean activo) {
        return (raiz, consulta, cb) -> {
            Predicate todo = cb.conjunction();
            if (texto != null && !texto.isBlank()) {
                String patron = "%" + escapar(texto.trim().toLowerCase(Locale.ROOT)) + "%";
                Expression<String> nombre = cb.lower(cb.concat(cb.concat(raiz.<String>get("nombres"), " "), raiz.<String>get("apellidos")));
                todo = cb.and(todo, cb.or(
                        cb.like(nombre, patron, '\\'),
                        cb.like(cb.lower(raiz.<String>get("email")), patron, '\\'),
                        cb.like(raiz.<String>get("dni"), patron, '\\')));
            }
            if (activo != null) {
                todo = cb.and(todo, cb.equal(raiz.get("activo"), activo));
            }
            return todo;
        };
    }

    /** Los comodines de LIKE escritos por el usuario se toman literalmente. */
    static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
