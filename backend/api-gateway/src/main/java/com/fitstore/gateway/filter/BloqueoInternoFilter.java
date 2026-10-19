package com.fitstore.gateway.filter;

import com.fitstore.gateway.web.RespuestasError;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/** Las rutas {@code /internal/**} son solo entre servicios: a través del gateway responden 404 (infraestructura 4.2). */
@Component
public class BloqueoInternoFilter implements WebFilter, Ordered {

    /** Después de la seguridad (-100) y antes del rate limiting. */
    public static final int ORDEN = -90;

    private final JsonMapper json;

    public BloqueoInternoFilter(JsonMapper json) {
        this.json = json;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange intercambio, WebFilterChain cadena) {
        String ruta = intercambio.getRequest().getPath().value();
        if (ruta.equals("/internal") || ruta.startsWith("/internal/")) {
            return RespuestasError.escribir(intercambio, json, HttpStatus.NOT_FOUND,
                    "No encontramos lo que buscas", "NO_ENCONTRADO");
        }
        return cadena.filter(intercambio);
    }

    @Override
    public int getOrder() {
        return ORDEN;
    }
}
