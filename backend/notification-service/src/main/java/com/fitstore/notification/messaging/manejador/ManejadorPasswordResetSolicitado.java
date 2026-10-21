package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.envio.CreadorEnvios;
import com.fitstore.notification.envio.Enlaces;
import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.PasswordResetSolicitado;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.VariablesCorreo;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** C2 (recuperación) o C3 (invitación de staff), según el motivo del evento; el botón lleva el enlace con el token. */
@Component
class ManejadorPasswordResetSolicitado implements ManejadorEvento<PasswordResetSolicitado> {

    private final CreadorEnvios envios;

    ManejadorPasswordResetSolicitado(CreadorEnvios envios) {
        this.envios = envios;
    }

    @Override
    public String tipo() {
        return "PasswordResetSolicitado";
    }

    @Override
    public Class<PasswordResetSolicitado> clase() {
        return PasswordResetSolicitado.class;
    }

    @Override
    public void manejar(UUID eventoId, PasswordResetSolicitado evento) {
        CatalogoPlantillas plantilla = "INVITACION".equals(evento.motivo())
                ? CatalogoPlantillas.INVITACION_STAFF : CatalogoPlantillas.RECUPERAR_PASSWORD;
        envios.crear(eventoId, plantilla, evento.email(),
                VariablesCorreo.nuevo().con("nombre", Enlaces.primerNombre(evento.nombres())).boton(evento.enlace())
                        .construir(), null, null);
    }
}
