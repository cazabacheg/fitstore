package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.Destinatario;

/** Destinatario de la entrega. DNI y teléfono solo los ve el staff; nunca van a los logs. */
public record DestinatarioEntregaDto(String nombres, String apellidos, String dni, String telefono) {

    public static DestinatarioEntregaDto de(Destinatario d) {
        return new DestinatarioEntregaDto(d.getNombres(), d.getApellidos(), d.getDni(), d.getTelefono());
    }
}
