package com.fitstore.order.pedidos.dto;

import com.fitstore.order.pedidos.model.Modalidad;
import java.math.BigDecimal;

/** Entrega del pedido (schema {@code Envio}); {@code tiendaDireccion} solo en recojo. */
public record EnvioDto(Modalidad modalidad, BigDecimal costo, Integer plazoDiasMin, Integer plazoDiasMax,
        Long agenciaId, String agenciaNombre, String codigoSeguimiento, String urlSeguimiento,
        String tiendaDireccion) {
}
