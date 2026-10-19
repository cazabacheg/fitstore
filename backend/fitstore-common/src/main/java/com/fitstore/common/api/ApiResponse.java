package com.fitstore.common.api;

/** Envoltorio de toda respuesta exitosa (ADR-015, contracts K4): {@code {success, mensaje, data}}. */
public record ApiResponse<T>(boolean success, String mensaje, T data) {

    public static final String MENSAJE_EXITO = "Operación exitosa";

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, MENSAJE_EXITO, data);
    }

    public static <T> ApiResponse<T> ok(String mensaje, T data) {
        return new ApiResponse<>(true, mensaje, data);
    }

    /** Respuesta exitosa sin datos y con un mensaje propio. */
    public static <T> ApiResponse<T> ok(String mensaje) {
        return new ApiResponse<>(true, mensaje, null);
    }
}
