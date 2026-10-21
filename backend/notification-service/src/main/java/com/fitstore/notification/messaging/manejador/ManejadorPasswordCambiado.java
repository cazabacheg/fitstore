package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.envio.CreadorEnvios;
import com.fitstore.notification.envio.Enlaces;
import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.PasswordCambiado;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.VariablesCorreo;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** C4: aviso de seguridad cuando cambia la contraseña. */
@Component
class ManejadorPasswordCambiado implements ManejadorEvento<PasswordCambiado> {

    private final CreadorEnvios envios;

    ManejadorPasswordCambiado(CreadorEnvios envios) {
        this.envios = envios;
    }

    @Override
    public String tipo() {
        return "PasswordCambiado";
    }

    @Override
    public Class<PasswordCambiado> clase() {
        return PasswordCambiado.class;
    }

    @Override
    public void manejar(UUID eventoId, PasswordCambiado evento) {
        envios.crear(eventoId, CatalogoPlantillas.PASSWORD_CAMBIADO, evento.email(),
                VariablesCorreo.nuevo().con("nombre", Enlaces.primerNombre(evento.nombres())).construir(), null, null);
    }
}
