package com.fitstore.delivery.dto;

import java.time.LocalDate;
import java.util.Map;

/** Entregas de un día por estado, para el dashboard (extensión de alcance). */
public record ResumenEntregasDto(LocalDate fecha, Map<String, Long> porEstado) {
}
