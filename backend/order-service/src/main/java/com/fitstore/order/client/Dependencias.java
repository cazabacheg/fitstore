package com.fitstore.order.client;

import com.fitstore.common.api.FitstoreException;
import feign.FeignException;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Traduce los fallos de las llamadas Feign a otros servicios (micro 7): una dependencia caída, sin respuesta o con el
 * circuito abierto se vuelve {@code 503 SERVICIO_NO_DISPONIBLE}; los errores 4xx se dejan pasar para que el servicio
 * decida qué significan.
 */
@Component
public class Dependencias {

    public static final String CODIGO_NO_DISPONIBLE = "SERVICIO_NO_DISPONIBLE";
    public static final String MENSAJE_NO_DISPONIBLE = "Servicio no disponible, inténtalo en un momento";

    /** Ejecuta la llamada; un fallo de infraestructura sale como {@link #noDisponible(Throwable)}. */
    public <T> T llamar(Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (FeignException e) {
            throw traducir(e);
        } catch (RuntimeException e) {
            FeignException feign = buscarFeign(e);
            if (feign != null) {
                throw traducir(feign);
            }
            throw noDisponible(e);
        }
    }

    /** El {@link FeignException} de una cadena de causas (el circuit breaker envuelve la excepción original). */
    public static FeignException buscarFeign(Throwable error) {
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if (actual instanceof FeignException feign) {
                return feign;
            }
            if (actual.getCause() == actual) {
                break;
            }
        }
        return null;
    }

    private RuntimeException traducir(FeignException e) {
        if (e.status() >= 500 || e.status() < 0) {
            return noDisponible(e);
        }
        return e;
    }

    public static FitstoreException noDisponible(Throwable causa) {
        FitstoreException error = new FitstoreException(HttpStatus.SERVICE_UNAVAILABLE, CODIGO_NO_DISPONIBLE,
                MENSAJE_NO_DISPONIBLE);
        error.initCause(causa);
        return error;
    }
}
