package com.fitstore.payment.client;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.payment.dto.ConfiguracionPublica;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/** Cliente de settings-service: configuración pública de la tienda (medios habilitados y datos del emisor). */
@FeignClient(name = "settings-service")
public interface SettingsClient {

    @GetMapping("/api/v1/settings/public")
    ApiResponse<ConfiguracionPublica> obtenerPublica();
}
