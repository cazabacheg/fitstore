package com.fitstore.notification.plantillas;

import com.fitstore.notification.model.Plantilla;

/** Los textos editables de un correo: asunto, título, mensaje (saltos de línea y **negrita**) y texto del botón. */
public record TextosPlantilla(String asunto, String titulo, String mensaje, String textoBoton) {

    public static TextosPlantilla de(Plantilla plantilla) {
        return new TextosPlantilla(plantilla.getAsunto(), plantilla.getTitulo(), plantilla.getMensaje(),
                plantilla.getTextoBoton());
    }
}
