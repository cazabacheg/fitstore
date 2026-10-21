package com.fitstore.delivery.dto;

import java.time.LocalDate;
import java.util.List;

/** Entregas de un día por repartidor y las sin asignar (extensión de alcance). */
public record EntregasDiaDto(LocalDate fecha, List<RutaRepartidorDto> rutas, List<ItemHojaRutaDto> sinAsignar) {
}
