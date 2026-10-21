package com.fitstore.payment.provider;

import com.fitstore.payment.config.PropiedadesPagos;
import com.fitstore.payment.model.Proveedor;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Un circuit breaker por proveedor (micro 7): si la pasarela falla repetidamente (sin respuesta o error del servidor)
 * se deja de ofrecer en la página de pago por un rato. Un pago rechazado no cuenta como fallo.
 */
@Component
public class CircuitoProveedores {

    private final Map<Proveedor, CircuitBreaker> circuitos = new ConcurrentHashMap<>();
    private final CircuitBreakerConfig configuracion;

    public CircuitoProveedores(PropiedadesPagos propiedades) {
        PropiedadesPagos.Circuito c = propiedades.circuito();
        this.configuracion = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(c.ventana())
                .minimumNumberOfCalls(c.minimoLlamadas())
                .failureRateThreshold(c.umbral())
                .waitDurationInOpenState(c.espera())
                .recordExceptions(PasarelaSinRespuestaException.class)
                .build();
    }

    private CircuitBreaker de(Proveedor proveedor) {
        return circuitos.computeIfAbsent(proveedor, p -> CircuitBreaker.of(p.name(), configuracion));
    }

    public boolean estaAbierto(Proveedor proveedor) {
        CircuitBreaker.State estado = de(proveedor).getState();
        return estado == CircuitBreaker.State.OPEN || estado == CircuitBreaker.State.FORCED_OPEN;
    }

    /** Ejecuta la llamada registrando su resultado; con el circuito abierto lanza {@link ProveedorNoDisponibleException}. */
    public <T> T ejecutar(Proveedor proveedor, Supplier<T> llamada) {
        try {
            return de(proveedor).executeSupplier(llamada);
        } catch (CallNotPermittedException e) {
            throw new ProveedorNoDisponibleException();
        }
    }
}
