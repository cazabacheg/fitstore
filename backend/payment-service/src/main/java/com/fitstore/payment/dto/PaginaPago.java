package com.fitstore.payment.dto;

import com.fitstore.payment.model.EstadoIntencion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Datos de la página de pago (contrato {@code PaginaPago}, CA-5-03 a CA-5-08). */
public record PaginaPago(String codigoPedido, EstadoIntencion estado, BigDecimal monto, List<ItemPago> items,
        BigDecimal costoEnvio, Instant expiraEn, List<MedioDisponible> medios, BigDecimal topeYape,
        List<IntentoRechazado> intentosRechazados, boolean procesando, boolean boletaDisponible,
        boolean simuladoHabilitado) {
}
