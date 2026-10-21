package com.fitstore.payment.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.payment.provider.WebhookEntrante;
import com.fitstore.payment.service.WebhookService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Avisos de las pasarelas: públicos, pero verificados por firma y confirmados consultando la API (D3). */
@RestController
@RequestMapping("/api/v1/pagos/webhooks")
public class WebhookController {

    private final WebhookService webhooks;

    public WebhookController(WebhookService webhooks) {
        this.webhooks = webhooks;
    }

    @PostMapping("/mercadopago")
    public ApiResponse<Void> recibirWebhookMercadoPago(@RequestHeader("x-signature") String firma,
            @RequestHeader("x-request-id") String idPeticion, @RequestBody Map<String, Object> cuerpo) {
        webhooks.recibirMercadoPago(new WebhookEntrante(firma, idPeticion, cuerpo));
        return ApiResponse.ok("Recibido");
    }
}
