package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.envio.CreadorEnvios;
import com.fitstore.notification.envio.Enlaces;
import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.EntregaActualizada;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.FormatoEsPe;
import com.fitstore.notification.plantillas.MarcaService;
import com.fitstore.notification.plantillas.MarcaVista;
import com.fitstore.notification.plantillas.VariablesCorreo;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Un solo evento de delivery, cinco correos según el estado: C8 en camino (EN_RUTA, ENVIADO_AGENCIA), C9 entrega
 * fallida, C10 listo para recoger (DISPONIBLE_EN_AGENCIA), C11 entregado y C14 pedido devuelto. Los estados
 * PENDIENTE_* y ASIGNADA no envían nada. Nunca se muestra el teléfono del repartidor.
 */
@Component
class ManejadorEntregaActualizada implements ManejadorEvento<EntregaActualizada> {

    private final CreadorEnvios envios;
    private final MarcaService marca;
    private final Enlaces enlaces;

    ManejadorEntregaActualizada(CreadorEnvios envios, MarcaService marca, Enlaces enlaces) {
        this.envios = envios;
        this.marca = marca;
        this.enlaces = enlaces;
    }

    @Override
    public String tipo() {
        return "EntregaActualizada";
    }

    @Override
    public Class<EntregaActualizada> clase() {
        return EntregaActualizada.class;
    }

    @Override
    public void manejar(UUID eventoId, EntregaActualizada evento) {
        VariablesCorreo variables = VariablesCorreo.nuevo()
                .con("nombre", Enlaces.primerNombre(evento.destinatario().nombre()))
                .con("codigoPedido", evento.codigoPedido());
        CatalogoPlantillas plantilla = switch (evento.estado()) {
            case "EN_RUTA", "ENVIADO_AGENCIA" -> {
                enCamino(variables, evento);
                yield CatalogoPlantillas.EN_CAMINO;
            }
            case "FALLIDA" -> {
                variables.con("observacion", evento.observacion()).fila("Observación", evento.observacion());
                yield CatalogoPlantillas.ENTREGA_FALLIDA;
            }
            case "DISPONIBLE_EN_AGENCIA" -> {
                variables.fila("Agencia", evento.agenciaNombre()).fila("Clave de recojo", evento.claveRecojo())
                        .boton(enlaces.pedido(evento.codigoPedido()));
                yield CatalogoPlantillas.LISTO_PARA_RECOGER;
            }
            case "ENTREGADA" -> CatalogoPlantillas.ENTREGADO;
            case "DEVUELTA" -> {
                devuelta(variables, evento);
                yield CatalogoPlantillas.PEDIDO_DEVUELTO;
            }
            default -> null; // PENDIENTE_ASIGNACION, ASIGNADA, PENDIENTE_ENVIO: sin correo
        };
        if (plantilla != null) {
            envios.crear(eventoId, plantilla, evento.destinatario().email(), variables.construir(),
                    evento.codigoPedido(), null);
        }
    }

    private void enCamino(VariablesCorreo variables, EntregaActualizada evento) {
        if ("AGENCIA".equals(evento.modalidad())) {
            variables.fila("Agencia", evento.agenciaNombre())
                    .fila("Código de seguimiento", evento.codigoSeguimiento())
                    .filaConEnlace("Rastrea tu envío", evento.urlSeguimiento(), evento.urlSeguimiento());
        } else {
            variables.fila("Fecha de entrega",
                    evento.fechaProgramada() == null ? null : FormatoEsPe.fecha(evento.fechaProgramada()))
                    .fila("Repartidor", evento.repartidorNombre());
        }
        variables.boton(enlaces.pedido(evento.codigoPedido()));
    }

    private void devuelta(VariablesCorreo variables, EntregaActualizada evento) {
        MarcaVista tienda = marca.actual();
        variables.con("motivo", evento.observacion())
                .fila("Motivo", evento.observacion())
                .fila("Escríbenos a", tienda.email())
                .fila("Llámanos al", tienda.telefono())
                .fila("Horario", tienda.horario());
    }
}
