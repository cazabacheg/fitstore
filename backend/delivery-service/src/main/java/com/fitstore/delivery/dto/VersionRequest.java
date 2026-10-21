package com.fitstore.delivery.dto;

import jakarta.validation.constraints.NotNull;

/** Avance sin datos extra: lleva la versión que vio el operador (E6). */
public record VersionRequest(@NotNull Integer version) {
}
