package com.fitstore.auth.event;

import com.fitstore.common.event.CatalogoRutasEventos;
import org.springframework.stereotype.Component;

/** Routing keys de los eventos que publica auth: las direcciones de sus canales en el AsyncAPI. */
@Component
public class CatalogoRutasEventosAuth implements CatalogoRutasEventos {

    @Override
    public String routingKey(String tipo) {
        return switch (tipo) {
            case "ClienteRegistrado" -> "auth.cliente.registrado";
            case "PasswordResetSolicitado" -> "auth.password.reset-solicitado";
            case "PasswordCambiado" -> "auth.password.cambiado";
            default -> throw new IllegalArgumentException("Evento desconocido en auth: " + tipo);
        };
    }
}
