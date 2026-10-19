package com.fitstore.gateway.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/** Cuerpo de error del gateway con el mismo formato {@code ApiError} de los servicios (contracts K5). */
public final class RespuestasError {

    public static final String SERVICIO_NO_DISPONIBLE = "Servicio no disponible, inténtalo en un momento";
    public static final String DEMASIADOS_INTENTOS = "Demasiados intentos, espera un momento";

    private RespuestasError() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Cuerpo(boolean success, String mensaje, String codigo, String requestId) {
    }

    public static Cuerpo cuerpo(String mensaje, String codigo, String requestId) {
        return new Cuerpo(false, mensaje, codigo, requestId);
    }

    /** Escribe el error JSON en la respuesta, con el {@code X-Request-Id} que ya puso la correlación. */
    public static Mono<Void> escribir(ServerWebExchange intercambio, JsonMapper json, HttpStatusCode estado,
            String mensaje, String codigo) {
        ServerHttpResponse respuesta = intercambio.getResponse();
        if (respuesta.isCommitted()) {
            return Mono.empty();
        }
        // el CorrelacionFilter lo puso en la petición; en la respuesta se agrega justo antes de enviarla
        String requestId = intercambio.getRequest().getHeaders().getFirst("X-Request-Id");
        byte[] bytes = json.writeValueAsString(cuerpo(mensaje, codigo, requestId)).getBytes(StandardCharsets.UTF_8);
        respuesta.setStatusCode(estado);
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        respuesta.getHeaders().setContentLength(bytes.length);
        DataBuffer buffer = respuesta.bufferFactory().wrap(bytes);
        return respuesta.writeWith(Mono.just(buffer));
    }
}
