package com.fitstore.notification.dto;

import java.time.Instant;

/** Un intento de envío; {@code error} nulo significa que salió en ese intento. */
public record IntentoDto(int numero, Instant fecha, String error) {
}
