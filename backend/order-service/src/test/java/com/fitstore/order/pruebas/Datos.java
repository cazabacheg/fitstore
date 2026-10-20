package com.fitstore.order.pruebas;

import com.fitstore.order.pedidos.model.Destinatario;
import com.fitstore.order.pedidos.model.DireccionEnvio;
import com.fitstore.order.pedidos.model.Envio;
import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.Pedido;
import com.fitstore.order.pedidos.model.PedidoItem;
import com.fitstore.order.pedidos.model.TipoActor;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Constructores de datos de prueba para las pruebas de capa de datos de order. */
public final class Datos {

    private Datos() {
    }

    /** Pedido de delivery local en {@code PENDIENTE_PAGO} con dos ítems y una fila de historial. */
    public static Pedido pedido(String codigo, UUID clienteId, String idempotencyKey) {
        Pedido pedido = base(codigo, clienteId, idempotencyKey, Modalidad.DELIVERY_LOCAL);
        pedido.setDireccion(new DireccionEnvio("150101", "Lima", "Lima", "Lima",
                "Jr. Gamarra 123", "Frente al mercado"));
        pedido.setEnvio(new Envio(null, null, 1, 2, null));
        pedido.agregarItem(item(10L, "POL-NEG-M", new BigDecimal("50.00"), 2));
        pedido.agregarItem(item(11L, "POL-BLA-L", new BigDecimal("30.00"), 1));
        return pedido;
    }

    /** Pedido de recojo en tienda: sin dirección ni envío. */
    public static Pedido pedidoRecojo(String codigo, UUID clienteId, String idempotencyKey) {
        Pedido pedido = base(codigo, clienteId, idempotencyKey, Modalidad.RECOJO);
        pedido.agregarItem(item(10L, "POL-NEG-M", new BigDecimal("50.00"), 1));
        return pedido;
    }

    public static PedidoItem item(long varianteId, String sku, BigDecimal precio, int cantidad) {
        PedidoItem item = new PedidoItem();
        item.setVarianteId(varianteId);
        item.setProductoId(varianteId / 10);
        item.setSku(sku);
        item.setProductoNombre("Polo deportivo");
        item.setTalla("M");
        item.setColor("Negro");
        item.setImagenUrl("http://media/polo.jpg");
        item.setPrecioUnitario(precio);
        item.setCantidad(cantidad);
        item.setDescuentoPorcentaje(BigDecimal.ZERO);
        item.setDescuentoMonto(BigDecimal.ZERO.setScale(2));
        item.setTotal(precio.multiply(BigDecimal.valueOf(cantidad)));
        return item;
    }

    private static Pedido base(String codigo, UUID clienteId, String idempotencyKey, Modalidad modalidad) {
        Pedido pedido = new Pedido();
        pedido.setCodigo(codigo);
        pedido.setClienteId(clienteId);
        pedido.setEstado(EstadoPedido.PENDIENTE_PAGO);
        pedido.setModalidad(modalidad);
        pedido.setDestinatario(new Destinatario("Ana", "Quispe", "12345678", "987654321", "ana@correo.pe"));
        pedido.setSubtotal(new BigDecimal("130.00"));
        pedido.setDescuentoTotal(new BigDecimal("10.00"));
        pedido.setCostoEnvio(new BigDecimal("8.00"));
        pedido.setTotal(new BigDecimal("128.00"));
        pedido.setExpiraEn(Instant.now().plus(30, ChronoUnit.MINUTES));
        pedido.setIdempotencyKey(idempotencyKey);
        pedido.agregarHistorial(null, EstadoPedido.PENDIENTE_PAGO, TipoActor.CLIENTE, clienteId, "Pedido creado");
        return pedido;
    }
}
