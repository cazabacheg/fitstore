package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.envio.CreadorEnvios;
import com.fitstore.notification.envio.Enlaces;
import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.PagoReembolsado;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.FormatoEsPe;
import com.fitstore.notification.plantillas.VariablesCorreo;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * C7: reembolso realizado. Si es parcial (postventa) o manual dice "reembolso parcial de tu pedido" con el monto y el
 * motivo (CA-7-06). Hoy ningún servicio publica reembolsos postventa, pero el manejador escucha el contrato.
 */
@Component
class ManejadorPagoReembolsado implements ManejadorEvento<PagoReembolsado> {

    private final CreadorEnvios envios;
    private final Enlaces enlaces;

    ManejadorPagoReembolsado(CreadorEnvios envios, Enlaces enlaces) {
        this.envios = envios;
        this.enlaces = enlaces;
    }

    @Override
    public String tipo() {
        return "PagoReembolsado";
    }

    @Override
    public Class<PagoReembolsado> clase() {
        return PagoReembolsado.class;
    }

    @Override
    public void manejar(UUID eventoId, PagoReembolsado evento) {
        String monto = FormatoEsPe.moneda(evento.monto());
        String motivo = motivo(evento.motivo());
        VariablesCorreo variables = VariablesCorreo.nuevo()
                .con("nombre", Enlaces.primerNombre(evento.cliente().nombre()))
                .con("codigoPedido", evento.codigoPedido())
                .con("monto", monto)
                .con("motivo", motivo);
        if (evento.parcial() || evento.manual()) {
            variables.fila("Tipo", "Reembolso parcial de tu pedido");
        }
        variables.fila("Monto", monto).fila("Motivo", motivo).boton(enlaces.pedido(evento.codigoPedido()));
        envios.crear(eventoId, CatalogoPlantillas.REEMBOLSO_REALIZADO, evento.cliente().email(), variables.construir(),
                evento.codigoPedido(), null);
    }

    private static String motivo(String motivo) {
        if (motivo == null) {
            return null;
        }
        return switch (motivo) {
            case "CANCELACION" -> "Cancelación del pedido";
            case "PAGO_NO_APLICABLE" -> "Pago que no se pudo aplicar al pedido";
            case "POSTVENTA" -> "Postventa";
            default -> motivo;
        };
    }
}
