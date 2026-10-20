package com.fitstore.catalog.catalogo.repository;

import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Variante;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros de la búsqueda de productos (micro 4.3). Los de categoría, talla y color usan {@code EXISTS}, no uniones:
 * un producto aparece una sola vez aunque coincida por varias categorías o variantes (D5).
 */
public final class ProductoSpecifications {

    private ProductoSpecifications() {
    }

    /** El producto pertenece a alguna de estas categorías (el llamador incluye las hijas con {@code idsConHijas}). */
    public static Specification<Producto> deCategorias(Collection<Long> categoriaIds) {
        return (producto, consulta, cb) -> {
            Subquery<Long> sub = consulta.subquery(Long.class);
            Root<Producto> interno = sub.correlate(producto);
            Join<Producto, Categoria> categoria = interno.join("categorias");
            sub.select(categoria.get("id")).where(categoria.get("id").in(categoriaIds));
            return cb.exists(sub);
        };
    }

    /** Tiene una variante activa de esa talla. */
    public static Specification<Producto> conTalla(Long tallaId) {
        return conVariante("tallaId", tallaId);
    }

    /** Tiene una variante activa de ese color. */
    public static Specification<Producto> conColor(Long colorId) {
        return conVariante("colorId", colorId);
    }

    private static Specification<Producto> conVariante(String campo, Long valor) {
        return (producto, consulta, cb) -> {
            Subquery<Long> sub = consulta.subquery(Long.class);
            Root<Variante> variante = sub.from(Variante.class);
            sub.select(variante.get("id")).where(
                    cb.equal(variante.get("productoId"), producto.get("id")),
                    cb.equal(variante.get(campo), valor),
                    cb.isTrue(variante.get("activo")));
            return cb.exists(sub);
        };
    }

    /** {@code precio_desde} entre los dos valores (cualquiera puede ser null). */
    public static Specification<Producto> precioEntre(BigDecimal minimo, BigDecimal maximo) {
        return (producto, consulta, cb) -> {
            var condiciones = new ArrayList<Predicate>();
            if (minimo != null) {
                condiciones.add(cb.greaterThanOrEqualTo(producto.<BigDecimal>get("precioDesde"), minimo));
            }
            if (maximo != null) {
                condiciones.add(cb.lessThanOrEqualTo(producto.<BigDecimal>get("precioDesde"), maximo));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    /** El nombre contiene el texto, sin importar tildes ni mayúsculas (usa el índice trigram de {@code f_unaccent}). */
    public static Specification<Producto> textoContiene(String texto) {
        return (producto, consulta, cb) -> {
            var nombre = cb.function("f_unaccent", String.class, cb.lower(producto.<String>get("nombre")));
            var buscado = cb.function("f_unaccent", String.class, cb.literal(texto.toLowerCase()));
            return cb.like(nombre, cb.concat(cb.concat("%", buscado), "%"));
        };
    }
}
