package com.fitstore.notification.dto;

import com.fitstore.notification.plantillas.CatalogoPlantillas;
import java.time.Instant;
import java.util.List;

/** Plantilla del catálogo con sus textos y su estado (extensión de alcance: solo la firma del contrato). */
public record PlantillaDto(String asunto, String titulo, String mensaje, String textoBoton, CatalogoPlantillas codigo,
        String nombre, String para, String evento, boolean activa, boolean obligatoria, List<String> variables,
        boolean personalizada, Instant actualizadaEn, String actualizadaPor) {
}
