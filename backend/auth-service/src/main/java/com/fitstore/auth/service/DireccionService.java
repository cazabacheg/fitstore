package com.fitstore.auth.service;

import com.fitstore.auth.dto.request.DireccionRequest;
import com.fitstore.auth.dto.response.DireccionResponse;
import java.util.List;
import java.util.UUID;

/**
 * Libreta de direcciones del cliente (micro 2.2 y 3.1, SPEC-1 A1 y A2). Toda consulta filtra por el cliente del token:
 * una dirección ajena responde 404, igual que una que no existe.
 */
public interface DireccionService {

    /** La predeterminada primero. */
    List<DireccionResponse> listar(UUID clienteId);

    DireccionResponse obtener(UUID clienteId, Long id);

    /** La primera queda como predeterminada; con 10 responde 409 {@code LIMITE_DIRECCIONES}. */
    DireccionResponse crear(UUID clienteId, DireccionRequest solicitud);

    DireccionResponse actualizar(UUID clienteId, Long id, DireccionRequest solicitud);

    /** Borrado físico; si era la predeterminada pasa a serlo la creada más recientemente (A2). */
    void eliminar(UUID clienteId, Long id);

    /** La anterior deja de serlo; devuelve la lista actualizada. */
    List<DireccionResponse> marcarPredeterminada(UUID clienteId, Long id);
}
