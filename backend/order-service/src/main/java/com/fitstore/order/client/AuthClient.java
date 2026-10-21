package com.fitstore.order.client;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.order.client.dto.DireccionCliente;
import com.fitstore.order.client.dto.PerfilCliente;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** auth-service: el perfil del cliente y una de sus direcciones, con el JWT del cliente propagado. */
@FeignClient(name = "auth-service", url = "${fitstore.clientes.auth.url:}")
public interface AuthClient {

    @GetMapping("/api/v1/clientes/me")
    ApiResponse<PerfilCliente> perfil();

    /** Una dirección ajena responde 404. */
    @GetMapping("/api/v1/clientes/me/direcciones/{id}")
    ApiResponse<DireccionCliente> direccion(@PathVariable("id") long id);
}
