package com.fitstore.gateway.web;

import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.TimeoutException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Convierte los errores del gateway al formato {@code ApiError}: sin ruta o servicio no encontrado → 404; servicio no
 * registrado, caído o que no responde a tiempo → 503 "Servicio no disponible, inténtalo en un momento".
 */
@Component
@Order(-2) // antes del manejador por defecto de Spring Boot (-1)
public class ManejadorErroresGateway implements ErrorWebExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErroresGateway.class);

    private final JsonMapper json;

    public ManejadorErroresGateway(JsonMapper json) {
        this.json = json;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange intercambio, Throwable error) {
        if (esServicioNoDisponible(error)) {
            LOG.warn("Servicio no disponible para {} {}: {}", intercambio.getRequest().getMethod(),
                    intercambio.getRequest().getPath(), error.toString());
            return RespuestasError.escribir(intercambio, json, HttpStatus.SERVICE_UNAVAILABLE,
                    RespuestasError.SERVICIO_NO_DISPONIBLE, "SERVICIO_NO_DISPONIBLE");
        }
        if (error instanceof ResponseStatusException e) {
            HttpStatusCode estado = e.getStatusCode();
            if (estado.value() == 404) {
                return RespuestasError.escribir(intercambio, json, estado, "No encontramos lo que buscas", "NO_ENCONTRADO");
            }
            if (estado.is4xxClientError()) {
                return RespuestasError.escribir(intercambio, json, estado, "Revisa los datos ingresados", "VALIDACION");
            }
        }
        LOG.error("Error inesperado en el gateway para {} {}", intercambio.getRequest().getMethod(),
                intercambio.getRequest().getPath(), error);
        return RespuestasError.escribir(intercambio, json, HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado, inténtalo en un momento", "ERROR_INTERNO");
    }

    private static boolean esServicioNoDisponible(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause() == causa ? null : causa.getCause()) {
            if (causa instanceof ResponseStatusException e) {
                int estado = e.getStatusCode().value();
                if (estado == 502 || estado == 503 || estado == 504) {
                    return true;
                }
            }
            if (causa instanceof ConnectException || causa instanceof ConnectTimeoutException
                    || causa instanceof TimeoutException || causa instanceof java.util.concurrent.TimeoutException
                    || causa instanceof UnknownHostException || causa instanceof SocketException
                    || causa instanceof PrematureCloseException) {
                return true;
            }
        }
        return false;
    }
}
