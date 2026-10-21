package com.fitstore.auth.dto.response;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Conjunto de claves públicas que publica auth; sin el envoltorio ApiResponse (formato estándar, K4). */
public record JwksResponse(@NotNull List<ClaveJwkResponse> keys) {
}
