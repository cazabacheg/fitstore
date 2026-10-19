package com.fitstore.gateway.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Respuesta 503 con el formato de error cuando un servicio de negocio no está disponible (infraestructura 4.5). */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/servicio-no-disponible")
    public ResponseEntity<RespuestasError.Cuerpo> servicioNoDisponible() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(RespuestasError.cuerpo(RespuestasError.SERVICIO_NO_DISPONIBLE, "SERVICIO_NO_DISPONIBLE", null));
    }
}
