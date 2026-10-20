package com.fitstore.order.pedidos.model;

/** Estados del pedido (micro order, sección 5). Las transiciones permitidas las define la máquina de estados (spec 4). */
public enum EstadoPedido {
    PENDIENTE_PAGO, PAGADO, EN_PREPARACION, LISTO_DESPACHO, LISTO_RECOJO, ENVIADO, ENTREGADO, CANCELADO, EXPIRADO, DEVUELTO
}
