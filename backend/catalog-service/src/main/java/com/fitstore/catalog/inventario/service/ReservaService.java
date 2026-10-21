package com.fitstore.catalog.inventario.service;

import com.fitstore.catalog.event.ItemCantidad;
import com.fitstore.catalog.inventario.dto.ReservaDto;
import com.fitstore.catalog.inventario.dto.ReservaRequest;
import java.util.List;
import java.util.UUID;

/**
 * Reserva de stock de un pedido y su ciclo de vida (micro 4.6 y 4.7, ADR-009): crear (T1), confirmar al pagar (T5),
 * liberar al cancelar o expirar, reingresar si se cancela un pedido pagado y liberar las vencidas.
 */
public interface ReservaService {

    /** Resultado de reservar: la reserva y si es nueva ({@code 201}) o ya existía ({@code 200}). */
    record Resultado(ReservaDto reserva, boolean creada) {
    }

    /**
     * Reserva todas las líneas en una sola transacción o ninguna (409 con los faltantes). Es idempotente por
     * {@code pedidoId}: repetirla devuelve la reserva existente.
     */
    Resultado reservar(ReservaRequest peticion);

    /** {@code PedidoPagado}: confirma la reserva ACTIVA; si ya no está activa, intenta tomar el stock (CA-2-49, 52). */
    void confirmarPago(UUID pedidoId, String codigoPedido, List<ItemCantidad> items);

    /** {@code PedidoCancelado}: libera la reserva ACTIVA o reingresa el stock de la CONFIRMADA (CA-2-50). */
    void cancelar(UUID pedidoId, String codigoPedido);

    /** {@code PedidoExpirado}: libera la reserva ACTIVA (CA-2-50). */
    void expirar(UUID pedidoId);

    /** Red de seguridad: libera las reservas ACTIVA cuya fecha ya pasó. @return cuántas liberó (CA-2-51) */
    int liberarVencidas();
}
