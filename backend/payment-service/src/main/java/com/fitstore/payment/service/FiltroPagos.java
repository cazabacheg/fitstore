package com.fitstore.payment.service;

import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.Proveedor;
import java.time.LocalDate;

/** Filtros del listado de pagos del backoffice; todos opcionales. */
public record FiltroPagos(EstadoIntencion estado, Proveedor proveedor, String codigo, LocalDate desde, LocalDate hasta) {
}
