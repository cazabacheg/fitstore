package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.EstadoEntrega;
import com.fitstore.delivery.model.Modalidad;
import java.time.LocalDate;

/** Filtros del listado de entregas; todos son opcionales y se combinan con Y (CA-6-32). */
public record FiltroEntregas(Modalidad modalidad, EstadoEntrega estado, LocalDate fecha, Long repartidorId, String codigo) {
}
