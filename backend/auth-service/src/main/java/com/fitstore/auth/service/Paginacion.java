package com.fitstore.auth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Arma el {@link Pageable} de un listado con los parámetros {@code page}, {@code size} y {@code sort} (contracts K6). */
public final class Paginacion {

    public static final int TAMANO_POR_DEFECTO = 20;
    public static final int TAMANO_MAXIMO = 100;

    private Paginacion() {
    }

    /**
     * @param sort      {@code campo,asc|desc}, repetible; los campos fuera de {@code permitidos} se ignoran
     * @param porDefecto orden cuando no llega ninguno válido
     */
    public static Pageable de(Integer page, Integer size, List<String> sort, Set<String> permitidos, Sort porDefecto) {
        int pagina = page == null ? 0 : Math.max(page, 0);
        int tamano = size == null ? TAMANO_POR_DEFECTO : Math.min(Math.max(size, 1), TAMANO_MAXIMO);
        Sort orden = ordenar(sort, permitidos);
        return PageRequest.of(pagina, tamano, orden.isUnsorted() ? porDefecto : orden);
    }

    /** Spring corta {@code email,asc} en dos valores: aquí se vuelven a unir ("asc" y "desc" califican al campo anterior). */
    private static Sort ordenar(List<String> sort, Set<String> permitidos) {
        if (sort == null) {
            return Sort.unsorted();
        }
        List<Sort.Order> ordenes = new ArrayList<>();
        String campo = null;
        for (String valor : sort) {
            for (String parte : valor.split(",")) {
                String limpio = parte.trim();
                if (limpio.isEmpty()) {
                    continue;
                }
                String minusculas = limpio.toLowerCase(Locale.ROOT);
                if (campo != null && (minusculas.equals("asc") || minusculas.equals("desc"))) {
                    ordenes.add(new Sort.Order(minusculas.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, campo));
                    campo = null;
                    continue;
                }
                if (campo != null) {
                    ordenes.add(Sort.Order.asc(campo));
                }
                campo = permitidos.contains(limpio) ? limpio : null;
            }
        }
        if (campo != null) {
            ordenes.add(Sort.Order.asc(campo));
        }
        return ordenes.isEmpty() ? Sort.unsorted() : Sort.by(ordenes);
    }
}
