package com.fitstore.settings.configuracion.dto;

import com.fitstore.settings.configuracion.model.MetodoPago;
import com.fitstore.settings.configuracion.model.ModalidadEntrega;
import java.util.List;

/** Parámetros de negocio que otros servicios pueden consultar (sin los correos para alertas). */
public record ParametrosPublicosDto(List<MetodoPago> metodosPago, List<ModalidadEntrega> modalidadesEntrega,
        int expiracionPedidoMinutos, int diasReembolsoPostventa, String moneda) {

    /** La moneda es fija (D4). */
    public static final String MONEDA = "PEN";
}
