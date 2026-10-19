package com.fitstore.common.event;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mensajería de un servicio (infraestructura 8).
 *
 * @param servicio        nombre corto del servicio, base de sus colas: {@code {servicio}.eventos} y {@code .dlq}
 * @param exchange        exchange topic común (por defecto {@code fitstore.eventos})
 * @param enlaces         routing keys a las que se enlaza la cola del servicio
 * @param reintentos      intentos de un consumidor antes de mandar el mensaje a la DLQ (por defecto 3)
 * @param esperaInicialMs espera tras el primer fallo (por defecto 1000 ms)
 * @param multiplicador   factor de crecimiento de la espera (por defecto 2)
 */
@ConfigurationProperties(prefix = "fitstore.eventos")
public record EventosProperties(String servicio, String exchange, List<String> enlaces,
        Integer reintentos, Long esperaInicialMs, Double multiplicador) {

    public static final String EXCHANGE_POR_DEFECTO = "fitstore.eventos";

    public EventosProperties {
        exchange = exchange == null ? EXCHANGE_POR_DEFECTO : exchange;
        enlaces = enlaces == null ? List.of() : List.copyOf(enlaces);
        reintentos = reintentos == null ? 3 : reintentos;
        esperaInicialMs = esperaInicialMs == null ? 1000L : esperaInicialMs;
        multiplicador = multiplicador == null ? 2.0 : multiplicador;
    }

    public String cola() {
        return servicio + ".eventos";
    }

    public String colaMuertos() {
        return cola() + ".dlq";
    }
}
