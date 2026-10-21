package com.fitstore.order.pedidos.service;

import com.fitstore.order.event.DestinatarioEvento;
import com.fitstore.order.event.DireccionEvento;
import com.fitstore.order.event.ItemCantidadEvento;
import com.fitstore.order.event.ItemPedidoEvento;
import com.fitstore.order.pedidos.dto.DestinatarioDto;
import com.fitstore.order.pedidos.dto.DireccionPedidoDto;
import com.fitstore.order.pedidos.dto.EnvioDto;
import com.fitstore.order.pedidos.dto.ItemPedidoDto;
import com.fitstore.order.pedidos.model.Destinatario;
import com.fitstore.order.pedidos.model.DireccionEnvio;
import com.fitstore.order.pedidos.model.Envio;
import com.fitstore.order.pedidos.model.Pedido;
import com.fitstore.order.pedidos.model.PedidoItem;
import java.math.BigDecimal;
import java.util.List;

/** Conversiones entre el pedido (copia congelada) y los DTO y eventos que salen de order. */
public final class PedidoMapper {

    private PedidoMapper() {
    }

    public static ItemPedidoDto item(PedidoItem item) {
        return new ItemPedidoDto(item.getVarianteId(), item.getProductoId(), item.getSku(), item.getProductoNombre(),
                item.getTalla(), item.getColor(), item.getImagenUrl(), item.getPrecioUnitario(), item.getCantidad(),
                item.getDescuentoPorcentaje(), item.getDescuentoMonto(), item.getTotal());
    }

    public static DireccionPedidoDto direccion(DireccionEnvio direccion) {
        return direccion == null ? null : new DireccionPedidoDto(direccion.getUbigeo(), direccion.getDepartamento(),
                direccion.getProvincia(), direccion.getDistrito(), direccion.getDireccion(), direccion.getReferencia());
    }

    public static DestinatarioDto destinatario(Destinatario d) {
        return new DestinatarioDto(d.getNombres(), d.getApellidos(), d.getDni(), d.getTelefono(), d.getEmail());
    }

    public static EnvioDto envio(Pedido pedido, String tiendaDireccion) {
        Envio envio = pedido.getEnvio();
        return new EnvioDto(pedido.getModalidad(), pedido.getCostoEnvio(),
                envio == null ? null : envio.getPlazoDiasMin(), envio == null ? null : envio.getPlazoDiasMax(),
                envio == null ? null : envio.getAgenciaId(), envio == null ? null : envio.getAgenciaNombre(),
                envio == null ? null : envio.getCodigoSeguimiento(), null, tiendaDireccion);
    }

    public static DestinatarioEvento destinatarioEvento(Destinatario d) {
        return new DestinatarioEvento(d.getNombres(), d.getApellidos(), d.getDni(), d.getTelefono(), d.getEmail());
    }

    public static DireccionEvento direccionEvento(DireccionEnvio d) {
        return d == null ? null : new DireccionEvento(d.getUbigeo(), d.getDepartamento(), d.getProvincia(),
                d.getDistrito(), d.getDireccion(), d.getReferencia());
    }

    public static List<ItemPedidoEvento> itemsEvento(Pedido pedido) {
        return pedido.getItems().stream()
                .map(i -> new ItemPedidoEvento(i.getVarianteId(), i.getProductoId(), i.getSku(), i.getProductoNombre(),
                        i.getTalla(), i.getColor(), i.getImagenUrl(), i.getPrecioUnitario(), i.getCantidad(),
                        i.getDescuentoPorcentaje(), i.getDescuentoMonto(), i.getTotal()))
                .toList();
    }

    public static List<ItemCantidadEvento> cantidades(Pedido pedido) {
        return pedido.getItems().stream().map(i -> new ItemCantidadEvento(i.getVarianteId(), i.getCantidad())).toList();
    }

    /** Estado del reembolso según lo devuelto frente al total (schema {@code EstadoReembolsoPedido}). */
    public static String estadoReembolso(BigDecimal reembolsado, BigDecimal total) {
        if (reembolsado == null || reembolsado.signum() == 0) {
            return "NINGUNO";
        }
        return reembolsado.compareTo(total) >= 0 ? "TOTAL" : "PARCIAL";
    }
}
