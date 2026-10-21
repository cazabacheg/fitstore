package com.fitstore.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades del módulo inventario ({@code fitstore.inventario.*}, micro 10.5).
 *
 * @param liberadorReservasCron cron del respaldo que libera reservas vencidas (cada minuto)
 * @param umbralUltimasUnidades hasta cuántas unidades se muestra "¡Últimas N unidades!"
 */
@ConfigurationProperties(prefix = "fitstore.inventario")
public record InventarioProperties(String liberadorReservasCron, Integer umbralUltimasUnidades) {

    public InventarioProperties {
        liberadorReservasCron = liberadorReservasCron == null ? "0 * * * * *" : liberadorReservasCron;
        umbralUltimasUnidades = umbralUltimasUnidades == null ? 5 : umbralUltimasUnidades;
    }
}
