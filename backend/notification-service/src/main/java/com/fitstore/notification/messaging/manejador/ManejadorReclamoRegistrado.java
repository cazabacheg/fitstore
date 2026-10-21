package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.envio.CreadorEnvios;
import com.fitstore.notification.envio.Enlaces;
import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.ReclamoRegistrado;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.FormatoEsPe;
import com.fitstore.notification.plantillas.VariablesCorreo;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * C12: copia de la hoja de reclamación para el consumidor (obligatoria por ley) y T4: alerta de reclamo nuevo para cada
 * correo de la tienda.
 */
@Component
class ManejadorReclamoRegistrado implements ManejadorEvento<ReclamoRegistrado> {

    private final CreadorEnvios envios;
    private final Enlaces enlaces;

    ManejadorReclamoRegistrado(CreadorEnvios envios, Enlaces enlaces) {
        this.envios = envios;
        this.enlaces = enlaces;
    }

    @Override
    public String tipo() {
        return "ReclamoRegistrado";
    }

    @Override
    public Class<ReclamoRegistrado> clase() {
        return ReclamoRegistrado.class;
    }

    @Override
    public void manejar(UUID eventoId, ReclamoRegistrado evento) {
        String tipo = "QUEJA".equals(evento.tipo()) ? "Queja" : "Reclamo";
        String limite = evento.fechaLimite() == null ? null : FormatoEsPe.fecha(evento.fechaLimite());
        String bien = evento.bien() == null ? null : evento.bien().entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(", "));

        VariablesCorreo copia = VariablesCorreo.nuevo()
                .con("nombre", Enlaces.primerNombre(evento.consumidor().nombre()))
                .con("numeroReclamo", evento.numero())
                .con("fechaLimite", limite)
                .fila("Número", evento.numero())
                .fila("Tipo", tipo)
                .fila("Bien contratado", bien)
                .fila("Detalle", evento.detalle())
                .fila("Pedido relacionado", evento.pedidoConsumidor())
                .fila("Fecha límite de respuesta", limite)
                .boton(enlaces.reclamos());
        envios.crear(eventoId, CatalogoPlantillas.RECLAMO_COPIA, evento.consumidor().email(), copia.construir(),
                evento.numero(), null);

        VariablesCorreo alerta = VariablesCorreo.nuevo()
                .con("numeroReclamo", evento.numero())
                .con("tipo", tipo)
                .con("fechaLimite", limite);
        envios.crearAlertas(eventoId, CatalogoPlantillas.ALERTA_RECLAMO_NUEVO, alerta.construir(), evento.numero());
    }
}
