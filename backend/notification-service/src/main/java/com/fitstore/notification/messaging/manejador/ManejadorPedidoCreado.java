package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.ItemPedido;
import com.fitstore.notification.messaging.evento.PedidoCreado;
import com.fitstore.notification.model.PedidoResumen;
import com.fitstore.notification.repository.PedidoResumenRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** No envía correo: guarda la copia local del pedido (sin DNI) para los correos que llegan después (D4). */
@Component
class ManejadorPedidoCreado implements ManejadorEvento<PedidoCreado> {

    private final PedidoResumenRepository pedidos;
    private final Clock reloj;

    ManejadorPedidoCreado(PedidoResumenRepository pedidos, Clock reloj) {
        this.pedidos = pedidos;
        this.reloj = reloj;
    }

    @Override
    public String tipo() {
        return "PedidoCreado";
    }

    @Override
    public Class<PedidoCreado> clase() {
        return PedidoCreado.class;
    }

    @Override
    public void manejar(UUID eventoId, PedidoCreado evento) {
        PedidoResumen resumen = pedidos.findById(evento.pedidoId()).orElseGet(PedidoResumen::new);
        resumen.setPedidoId(evento.pedidoId());
        resumen.setCodigo(evento.codigo());
        resumen.setEmail(evento.destinatario().email());
        resumen.setNombre(evento.destinatario().nombres());
        resumen.setItems(items(evento.items()));
        resumen.setTotales(totales(evento));
        resumen.setModalidad(evento.modalidad());
        if (resumen.getCreadoEn() == null) {
            resumen.setCreadoEn(reloj.instant());
        }
        pedidos.save(resumen);
    }

    private static List<Map<String, Object>> items(List<ItemPedido> items) {
        List<Map<String, Object>> filas = new ArrayList<>();
        for (ItemPedido item : items) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("producto", item.producto());
            fila.put("talla", item.talla());
            fila.put("color", item.color());
            fila.put("cantidad", item.cantidad());
            fila.put("precioUnitario", item.precioUnitario().toPlainString());
            fila.put("total", item.total().toPlainString());
            filas.add(fila);
        }
        return filas;
    }

    private static Map<String, Object> totales(PedidoCreado evento) {
        Map<String, Object> totales = new LinkedHashMap<>();
        totales.put("subtotal", evento.subtotal().toPlainString());
        totales.put("descuentoTotal", evento.descuentoTotal().toPlainString());
        totales.put("costoEnvio", evento.costoEnvio().toPlainString());
        totales.put("total", evento.total().toPlainString());
        return totales;
    }
}
