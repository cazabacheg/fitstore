package com.fitstore.auth.service;

import com.fitstore.auth.dto.request.PerfilClienteRequest;
import com.fitstore.auth.dto.response.ClienteDetalleResponse;
import com.fitstore.auth.dto.response.ClienteResponse;
import com.fitstore.auth.dto.response.ClienteResumenResponse;
import com.fitstore.common.api.PaginaResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/** Perfil del cliente y consulta de clientes para el backoffice (micro 4.1 y 4.6). */
public interface ClienteService {

    ClienteResponse perfil(UUID clienteId);

    /** Actualiza nombres, apellidos y celular; el correo y el DNI no cambian. */
    ClienteResponse actualizarPerfil(UUID clienteId, PerfilClienteRequest solicitud);

    /** Búsqueda del backoffice por nombre, correo o DNI, con filtro opcional por estado. */
    PaginaResponse<ClienteResumenResponse> buscar(String texto, Boolean activo, Pageable pagina);

    /** Cliente con sus direcciones (la predeterminada primero). */
    ClienteDetalleResponse detalle(UUID id);

    /** Desactiva al cliente: no puede ingresar ni refrescar su sesión. */
    ClienteDetalleResponse bloquear(UUID id);

    ClienteDetalleResponse desbloquear(UUID id);
}
