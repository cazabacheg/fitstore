package com.fitstore.gateway.filter;

import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Asigna el {@code X-Request-Id} (id de correlación): lo genera el gateway (nunca confía en el del cliente), lo envía
 * al servicio y lo devuelve en la respuesta, incluidas las de error (401, 404, 429, 503). Es el primer filtro de la
 * cadena para que ninguna respuesta salga sin él (contracts K9).
 */
@Component
public class CorrelacionFilter implements WebFilter, Ordered {

    public static final String CABECERA = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange intercambio, WebFilterChain cadena) {
        String requestId = UUID.randomUUID().toString();
        ServerHttpRequest peticion = intercambio.getRequest().mutate().header(CABECERA, requestId).build();
        // Se fija justo antes de enviar la respuesta para reemplazar cualquier valor que haya puesto el servicio
        intercambio.getResponse().beforeCommit(() -> {
            intercambio.getResponse().getHeaders().set(CABECERA, requestId);
            return Mono.empty();
        });
        return cadena.filter(intercambio.mutate().request(peticion).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
