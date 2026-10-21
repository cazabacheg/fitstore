package com.fitstore.order.pedidos.dto;

import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Pedido para el backoffice (schema {@code PedidoAdmin}): el detalle más id, cliente, acciones y {@code version}. */
public record PedidoAdminDto(String codigo, EstadoPedido estado, Modalidad modalidad, Instant creadoEn,
        Instant expiraEn, List<ItemPedidoDto> items, BigDecimal subtotal, BigDecimal descuentoTotal,
        BigDecimal costoEnvio, BigDecimal total, BigDecimal montoReembolsado, String estadoReembolso,
        DestinatarioDto destinatario, DireccionPedidoDto direccion, EnvioDto envio, String notas, String boletaUrl,
        PagoPedidoDto pago, List<HistorialEntradaDto> historial, boolean puedeCancelar, boolean puedePagar,
        UUID id, UUID clienteId, List<TransicionPermitidaDto> transicionesPermitidas, boolean puedeCancelarStaff,
        ReembolsoPostventaInfoDto reembolsoPostventa, List<ReembolsoDelPedidoDto> reembolsos, long version) {
}
