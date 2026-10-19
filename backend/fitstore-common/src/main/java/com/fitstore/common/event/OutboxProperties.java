package com.fitstore.common.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Publicador del outbox.
 *
 * @param habilitado           activa el registrador y el publicador programado
 * @param intervaloMs          pausa entre ejecuciones del publicador (por defecto 1000 ms)
 * @param lote                 filas pendientes que toma en cada ejecución (por defecto 100)
 * @param esperaConfirmacionMs tiempo máximo que espera la confirmación de RabbitMQ (por defecto 5000 ms)
 */
@ConfigurationProperties(prefix = "fitstore.outbox")
public record OutboxProperties(Boolean habilitado, Long intervaloMs, Integer lote, Long esperaConfirmacionMs) {

    public OutboxProperties {
        intervaloMs = intervaloMs == null ? 1000L : intervaloMs;
        lote = lote == null ? 100 : lote;
        esperaConfirmacionMs = esperaConfirmacionMs == null ? 5000L : esperaConfirmacionMs;
    }
}
