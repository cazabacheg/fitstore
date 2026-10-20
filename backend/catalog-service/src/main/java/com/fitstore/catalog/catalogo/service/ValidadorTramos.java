package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.TramoDto;
import com.fitstore.common.api.FitstoreException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Reglas de los tramos de descuento (micro invariante 7), válidas para la política global y para los tramos propios de un
 * producto: hasta 10, unidades estrictamente crecientes y porcentajes crecientes entre 0 y 50 %.
 */
final class ValidadorTramos {

    private ValidadorTramos() {
    }

    static void validar(List<TramoDto> tramos, int maximo, int porcentajeMaximo) {
        if (tramos.size() > maximo) {
            throw rechazo("Máximo " + maximo + " tramos de descuento");
        }
        for (int i = 0; i < tramos.size(); i++) {
            TramoDto actual = tramos.get(i);
            if (actual.porcentaje().signum() < 0 || actual.porcentaje().compareTo(BigDecimal.valueOf(porcentajeMaximo)) > 0) {
                throw rechazo("El porcentaje de cada tramo debe estar entre 0 y " + porcentajeMaximo);
            }
            if (i > 0) {
                TramoDto anterior = tramos.get(i - 1);
                if (actual.unidadesMinimas() <= anterior.unidadesMinimas()) {
                    throw rechazo("Las unidades mínimas de los tramos deben ser crecientes");
                }
                if (actual.porcentaje().compareTo(anterior.porcentaje()) <= 0) {
                    throw rechazo("Los porcentajes de los tramos deben ser crecientes");
                }
            }
        }
    }

    private static FitstoreException rechazo(String mensaje) {
        return new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION", mensaje);
    }
}
