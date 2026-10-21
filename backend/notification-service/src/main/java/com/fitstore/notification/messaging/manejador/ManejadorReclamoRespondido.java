package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.envio.CreadorEnvios;
import com.fitstore.notification.envio.Enlaces;
import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.ReclamoRespondido;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.VariablesCorreo;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** C13: respuesta a un reclamo (obligatoria por ley). */
@Component
class ManejadorReclamoRespondido implements ManejadorEvento<ReclamoRespondido> {

    private final CreadorEnvios envios;
    private final Enlaces enlaces;

    ManejadorReclamoRespondido(CreadorEnvios envios, Enlaces enlaces) {
        this.envios = envios;
        this.enlaces = enlaces;
    }

    @Override
    public String tipo() {
        return "ReclamoRespondido";
    }

    @Override
    public Class<ReclamoRespondido> clase() {
        return ReclamoRespondido.class;
    }

    @Override
    public void manejar(UUID eventoId, ReclamoRespondido evento) {
        VariablesCorreo variables = VariablesCorreo.nuevo()
                .con("nombre", Enlaces.primerNombre(evento.consumidor().nombre()))
                .con("numeroReclamo", evento.numero())
                .fila("Respuesta", evento.respuesta())
                .boton(enlaces.reclamos());
        envios.crear(eventoId, CatalogoPlantillas.RECLAMO_RESPUESTA, evento.consumidor().email(), variables.construir(),
                evento.numero(), null);
    }
}
