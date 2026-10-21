package com.fitstore.payment.provider;

import java.util.Map;

/** Un webhook tal como llegó: cabeceras de firma y cuerpo ya leído. */
public record WebhookEntrante(String firma, String idPeticion, Map<String, Object> cuerpo) {
}
