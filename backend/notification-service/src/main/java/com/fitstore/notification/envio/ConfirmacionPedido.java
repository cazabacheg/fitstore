package com.fitstore.notification.envio;

import com.fitstore.notification.config.PropiedadesNotificacion;
import com.fitstore.notification.model.Modalidad;
import com.fitstore.notification.model.PedidoResumen;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.FormatoEsPe;
import com.fitstore.notification.plantillas.VariablesCorreo;
import com.fitstore.notification.repository.PedidoResumenRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * "¡Pedido confirmado!" (C5, micro 4.3): con la boleta adjunta cuando llega {@code BoletaEmitida}, o sin ella cuando
 * vence el respaldo de 10 minutos. El pedido guarda que ya se confirmó, así que nunca salen dos confirmaciones.
 */
@Component
public class ConfirmacionPedido {

    private final CreadorEnvios creador;
    private final PedidoResumenRepository pedidos;
    private final PropiedadesNotificacion propiedades;

    public ConfirmacionPedido(CreadorEnvios creador, PedidoResumenRepository pedidos, PropiedadesNotificacion propiedades) {
        this.creador = creador;
        this.pedidos = pedidos;
        this.propiedades = propiedades;
    }

    /** @param numeroBoleta boleta a adjuntar, o nulo para el respaldo sin adjunto */
    public void confirmar(UUID eventoId, PedidoResumen pedido, String numeroBoleta) {
        if (pedido.isConfirmacionEnviada()) {
            return;
        }
        pedido.setConfirmacionEnviada(true);
        pedidos.save(pedido);
        Map<String, Object> totales = pedido.getTotales();
        VariablesCorreo variables = VariablesCorreo.nuevo()
                .con("nombre", pedido.getNombre())
                .con("codigoPedido", pedido.getCodigo())
                .con("total", moneda(totales.get("total")))
                .con("subtotal", moneda(totales.get("subtotal")))
                .con("descuento", moneda(totales.get("descuentoTotal")))
                .con("costoEnvio", moneda(totales.get("costoEnvio")))
                .con("modalidad", etiqueta(pedido.getModalidad()))
                .con(VariablesCorreo.ITEMS, items(pedido.getItems()))
                .boton(propiedades.enlaces().storefront() + "/cuenta/pedidos/" + pedido.getCodigo());
        if (numeroBoleta == null) {
            variables.con(VariablesCorreo.SIN_BOLETA, true);
        }
        creador.crear(eventoId, CatalogoPlantillas.PEDIDO_CONFIRMADO, pedido.getEmail(), variables.construir(),
                pedido.getCodigo(), numeroBoleta);
    }

    private static List<Map<String, Object>> items(List<Map<String, Object>> guardados) {
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Map<String, Object> item : guardados) {
            filas.add(Map.of("producto", String.valueOf(item.get("producto")), "talla", String.valueOf(item.get("talla")),
                    "color", String.valueOf(item.get("color")), "cantidad", item.get("cantidad"),
                    "precioUnitario", moneda(item.get("precioUnitario")), "total", moneda(item.get("total"))));
        }
        return filas;
    }

    private static String moneda(Object monto) {
        return monto == null ? null : FormatoEsPe.moneda(new BigDecimal(String.valueOf(monto)));
    }

    public static String etiqueta(Modalidad modalidad) {
        return switch (modalidad) {
            case DELIVERY_LOCAL -> "Delivery a domicilio";
            case AGENCIA -> "Envío por agencia";
            case RECOJO -> "Recojo en tienda";
        };
    }
}
