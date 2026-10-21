package com.fitstore.delivery.pruebas;

import com.fitstore.delivery.event.PedidoListoDespacho;
import java.util.UUID;

/** Fábricas de {@code PedidoListoDespacho} para las pruebas de entregas. */
public final class Pedidos {

    private Pedidos() {
    }

    private static PedidoListoDespacho.Destinatario destinatario() {
        return new PedidoListoDespacho.Destinatario("Lucía", "Mendoza Ríos", "45678912", "955111222", "lucia@correo.pe");
    }

    /** Pedido para delivery local en Miraflores. */
    public static PedidoListoDespacho local(String codigo) {
        return new PedidoListoDespacho(UUID.randomUUID(), codigo, "DELIVERY_LOCAL", destinatario(),
                new PedidoListoDespacho.Direccion("150122", "Lima", "Lima", "Miraflores", "Av. Larco 345 dpto 201",
                        "Frente al parque Kennedy"),
                null, null, "Legging Pro Compresión M Negro × 2 · Top Flex S Blanco × 1", 2);
    }

    /** Pedido por agencia a Arequipa; la agencia es la que eligió el cliente. */
    public static PedidoListoDespacho agencia(String codigo, Long agenciaId, String agenciaNombre) {
        return new PedidoListoDespacho(UUID.randomUUID(), codigo, "AGENCIA", destinatario(),
                new PedidoListoDespacho.Direccion("040101", "Arequipa", "Arequipa", "Arequipa", "Calle Mercaderes 120",
                        null),
                agenciaId, agenciaNombre, "Polo Dry Fit L Azul × 3", 1);
    }
}
