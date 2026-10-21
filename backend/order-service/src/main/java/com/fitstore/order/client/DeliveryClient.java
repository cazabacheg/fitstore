package com.fitstore.order.client;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.order.client.dto.CotizacionEnvio;
import com.fitstore.order.client.dto.CotizarEnvioRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** delivery-service: costo y plazo de un envío según la modalidad, el distrito y la agencia. */
@FeignClient(name = "delivery-service", url = "${fitstore.clientes.delivery.url:}")
public interface DeliveryClient {

    /** Sin tarifa para el destino responde 422 {@code SIN_COBERTURA}. */
    @PostMapping("/api/v1/envios/cotizar")
    ApiResponse<CotizacionEnvio> cotizar(@RequestBody CotizarEnvioRequest peticion);
}
