package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.envio.CreadorEnvios;
import com.fitstore.notification.envio.Enlaces;
import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.ClienteRegistrado;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.VariablesCorreo;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** C1: bienvenida al cliente que se registra. */
@Component
class ManejadorClienteRegistrado implements ManejadorEvento<ClienteRegistrado> {

    private final CreadorEnvios envios;
    private final Enlaces enlaces;

    ManejadorClienteRegistrado(CreadorEnvios envios, Enlaces enlaces) {
        this.envios = envios;
        this.enlaces = enlaces;
    }

    @Override
    public String tipo() {
        return "ClienteRegistrado";
    }

    @Override
    public Class<ClienteRegistrado> clase() {
        return ClienteRegistrado.class;
    }

    @Override
    public void manejar(UUID eventoId, ClienteRegistrado evento) {
        envios.crear(eventoId, CatalogoPlantillas.BIENVENIDA, evento.email(),
                VariablesCorreo.nuevo().con("nombre", Enlaces.primerNombre(evento.nombres())).boton(enlaces.tienda())
                        .construir(), null, null);
    }
}
