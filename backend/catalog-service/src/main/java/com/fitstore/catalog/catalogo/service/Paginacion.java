package com.fitstore.catalog.catalogo.service;

import com.fitstore.common.api.FitstoreException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

/** Arma la paginación del backoffice a partir de {@code page}, {@code size} y {@code sort=campo,dir} (contracts K6). */
public final class Paginacion {

    private Paginacion() {
    }

    /**
     * Spring parte {@code sort=nombre,asc} en {@code [nombre, asc]}: aquí se vuelve a unir cada dirección con su campo.
     */
    private static List<String> unirDirecciones(List<String> partes) {
        List<String> criterios = new ArrayList<>();
        for (String parte : partes) {
            boolean esDireccion = "asc".equalsIgnoreCase(parte) || "desc".equalsIgnoreCase(parte);
            if (esDireccion && !criterios.isEmpty() && !criterios.get(criterios.size() - 1).contains(",")) {
                criterios.set(criterios.size() - 1, criterios.get(criterios.size() - 1) + "," + parte);
            } else {
                criterios.add(parte);
            }
        }
        return criterios;
    }

    /**
     * @param permitidos campo del API → propiedad de la entidad; un campo desconocido responde 400
     * @param porDefecto orden cuando no se pide ninguno
     */
    public static Pageable de(int pagina, int tamano, List<String> orden, Map<String, String> permitidos, Sort porDefecto) {
        if (orden == null || orden.isEmpty()) {
            return PageRequest.of(pagina, tamano, porDefecto);
        }
        List<Sort.Order> ordenes = new ArrayList<>();
        for (String criterio : unirDirecciones(orden)) {
            String[] partes = criterio.split(",");
            String propiedad = permitidos.get(partes[0]);
            if (propiedad == null) {
                throw new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION", "No se puede ordenar por " + partes[0]);
            }
            boolean descendente = partes.length > 1 && "desc".equalsIgnoreCase(partes[1]);
            ordenes.add(descendente ? Sort.Order.desc(propiedad) : Sort.Order.asc(propiedad));
        }
        return PageRequest.of(pagina, tamano, Sort.by(ordenes));
    }
}
