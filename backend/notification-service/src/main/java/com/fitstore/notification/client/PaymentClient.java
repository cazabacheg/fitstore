package com.fitstore.notification.client;

import com.fitstore.notification.config.PropiedadesNotificacion;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Llamada a payment para adjuntar la boleta (micro 4.3 y 7): responde en 5 s o el envío se reintenta. */
@Component
public class PaymentClient {

    private final RestClient cliente;
    private final String base;

    public PaymentClient(RestClient clienteSaliente, PropiedadesNotificacion propiedades) {
        this.cliente = clienteSaliente;
        this.base = propiedades.paymentUrl() == null ? "" : propiedades.paymentUrl().replaceAll("/+$", "");
    }

    /** @return el PDF de la boleta {@code B001-xxxxxxxx} */
    public byte[] descargarBoleta(String numero) {
        if (base.isBlank()) {
            throw new IllegalStateException("fitstore.notificacion.payment-url no está configurada");
        }
        byte[] pdf = cliente.get().uri(base + "/internal/v1/boletas/{numero}", numero).retrieve().body(byte[].class);
        if (pdf == null || pdf.length == 0) {
            throw new IllegalStateException("Payment devolvió la boleta " + numero + " vacía");
        }
        return pdf;
    }
}
