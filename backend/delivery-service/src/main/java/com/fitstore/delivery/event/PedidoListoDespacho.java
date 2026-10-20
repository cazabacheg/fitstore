package com.fitstore.delivery.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * Evento que consume delivery ({@code order.pedido.listo-despacho}): contenido {@code datos} del sobre K12.
 * Ignora los campos que no conoce (compatibilidad hacia adelante).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoListoDespacho(UUID pedidoId, String codigo, String modalidad, Destinatario destinatario,
        Direccion direccion, Long agenciaId, String agenciaNombre, String resumenItems, int bultos) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Destinatario(String nombres, String apellidos, String dni, String telefono, String email) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Direccion(String ubigeo, String departamento, String provincia, String distrito, String direccion,
            String referencia) {
    }
}
