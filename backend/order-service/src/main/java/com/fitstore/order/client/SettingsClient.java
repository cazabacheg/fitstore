package com.fitstore.order.client;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.order.client.dto.ConfiguracionPublica;
import com.fitstore.order.client.dto.UbigeoNombre;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** settings-service: parámetros públicos de la tienda y nombres del ubigeo. */
@FeignClient(name = "settings-service", url = "${fitstore.clientes.settings.url:}")
public interface SettingsClient {

    @GetMapping("/api/v1/settings/public")
    ApiResponse<ConfiguracionPublica> configuracionPublica();

    @GetMapping("/api/v1/ubigeo/{codigo}")
    ApiResponse<UbigeoNombre> ubigeo(@PathVariable("codigo") String codigo);
}
