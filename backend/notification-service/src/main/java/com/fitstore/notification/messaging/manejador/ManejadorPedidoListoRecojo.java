package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.envio.CreadorEnvios;
import com.fitstore.notification.envio.Enlaces;
import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.PedidoListoRecojo;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.MarcaService;
import com.fitstore.notification.plantillas.MarcaVista;
import com.fitstore.notification.plantillas.VariablesCorreo;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** C10: el pedido de recojo en tienda está listo; indica dónde y cuándo (la tienda de la marca) y "lleva tu DNI". */
@Component
class ManejadorPedidoListoRecojo implements ManejadorEvento<PedidoListoRecojo> {

    private final CreadorEnvios envios;
    private final MarcaService marca;
    private final Enlaces enlaces;

    ManejadorPedidoListoRecojo(CreadorEnvios envios, MarcaService marca, Enlaces enlaces) {
        this.envios = envios;
        this.marca = marca;
        this.enlaces = enlaces;
    }

    @Override
    public String tipo() {
        return "PedidoListoRecojo";
    }

    @Override
    public Class<PedidoListoRecojo> clase() {
        return PedidoListoRecojo.class;
    }

    @Override
    public void manejar(UUID eventoId, PedidoListoRecojo evento) {
        MarcaVista tienda = marca.actual();
        VariablesCorreo variables = VariablesCorreo.nuevo()
                .con("nombre", evento.destinatario().nombres())
                .con("codigoPedido", evento.codigo())
                .fila("Dónde recoger", tienda.direccion())
                .fila("Horario", tienda.horario())
                .boton(enlaces.pedido(evento.codigo()));
        envios.crear(eventoId, CatalogoPlantillas.LISTO_PARA_RECOGER, evento.destinatario().email(),
                variables.construir(), evento.codigo(), null);
    }
}
