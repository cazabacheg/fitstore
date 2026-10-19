package com.fitstore.common.api;

import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Manejador de errores que registra la autoconfiguración cuando el servicio no define el suyo. */
@RestControllerAdvice
class ManejadorErroresPorDefecto extends GlobalExceptionHandlerBase {
}
