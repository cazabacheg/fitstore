package com.fitstore.payment.event;

import com.fitstore.common.event.CatalogoRutasEventos;
import org.springframework.stereotype.Component;

/** Routing keys de los eventos que publica payment: las direcciones de sus canales en el AsyncAPI. */
@Component
public class CatalogoRutasEventosPayment implements CatalogoRutasEventos {

    @Override
    public String routingKey(String tipo) {
        return switch (tipo) {
            case "PagoConfirmado" -> "payment.pago.confirmado";
            case "PagoRechazado" -> "payment.pago.rechazado";
            case "BoletaEmitida" -> "payment.boleta.emitida";
            case "PagoReembolsado" -> "payment.reembolso.completado";
            case "ReembolsoFallido" -> "payment.reembolso.fallido";
            default -> throw new IllegalArgumentException("Evento desconocido en payment: " + tipo);
        };
    }
}
