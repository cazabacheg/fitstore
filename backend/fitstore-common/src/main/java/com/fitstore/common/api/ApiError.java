package com.fitstore.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/**
 * Respuesta de error (contracts K5): {@code {success: false, mensaje, codigo, errores[], requestId, detalles}}.
 * {@code errores}, {@code requestId} y {@code detalles} se omiten cuando no existen.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        boolean success,
        String mensaje,
        String codigo,
        List<ErrorCampo> errores,
        String requestId,
        Map<String, Object> detalles) {

    public static ApiError de(String mensaje, String codigo) {
        return new ApiError(false, mensaje, codigo, null, null, null);
    }

    public ApiError conErrores(List<ErrorCampo> nuevosErrores) {
        return new ApiError(false, mensaje, codigo, nuevosErrores, requestId, detalles);
    }

    public ApiError conRequestId(String nuevoRequestId) {
        return new ApiError(false, mensaje, codigo, errores, nuevoRequestId, detalles);
    }

    public ApiError conDetalles(Map<String, Object> nuevosDetalles) {
        return new ApiError(false, mensaje, codigo, errores, requestId, nuevosDetalles);
    }
}
