package com.fitstore.auth.service;

import com.fitstore.auth.dto.request.PerfilClienteRequest;
import com.fitstore.auth.dto.response.ClienteResponse;
import java.util.UUID;

/** Perfil del cliente y consulta de clientes para el backoffice (micro 4.1 y 4.6). */
public interface ClienteService {

    ClienteResponse perfil(UUID clienteId);

    /** Actualiza nombres, apellidos y celular; el correo y el DNI no cambian. */
    ClienteResponse actualizarPerfil(UUID clienteId, PerfilClienteRequest solicitud);
}
