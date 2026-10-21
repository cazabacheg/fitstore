package com.fitstore.payment.config;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración técnica de los pagos ({@code fitstore.pagos.*}, micro 10.5). Las credenciales vienen de variables de
 * entorno; un proveedor sin credenciales no se ofrece en la página de pago.
 */
@ConfigurationProperties(prefix = "fitstore.pagos")
public record PropiedadesPagos(
        @DefaultValue MercadoPago mercadopago,
        @DefaultValue Simulado simulado,
        @DefaultValue Yape yape,
        @DefaultValue Verificador verificador,
        @DefaultValue Boleta boleta,
        @DefaultValue Circuito circuito,
        @DefaultValue("5m") Duration settingsCacheTtl) {

    /** Mercado Pago (sandbox): clave pública para el Payment Brick, token de acceso y secreto de los webhooks. */
    public record MercadoPago(
            String publicKey,
            String accessToken,
            String webhookSecret,
            @DefaultValue("https://api.mercadopago.com") String url,
            @DefaultValue("10s") Duration timeout) {

        public boolean tieneCredenciales() {
            return publicKey != null && !publicKey.isBlank() && accessToken != null && !accessToken.isBlank();
        }
    }

    /** Proveedor simulado de la demo: solo existe con {@code habilitado=true} (D7). */
    public record Simulado(boolean habilitado) {
    }

    /** Tope diario de Yape en soles (P6). */
    public record Yape(@DefaultValue("500.00") BigDecimal topeDiario) {
    }

    /** Verificador de pagos en curso: cada cuánto corre y qué antigüedad mínima tiene lo que consulta. */
    public record Verificador(@DefaultValue("2m") Duration cada, @DefaultValue("2m") Duration antiguedad) {
    }

    /** Boleta: serie, leyenda legal y cada cuánto se emiten las pendientes. */
    public record Boleta(
            @DefaultValue("B001") String serie,
            @DefaultValue("Documento simulado sin validez tributaria") String leyenda,
            @DefaultValue("20s") Duration cada) {
    }

    /** Circuit breaker por proveedor: ventana de llamadas, mínimo para evaluar, umbral de fallos y espera abierto. */
    public record Circuito(
            @DefaultValue("10") int ventana,
            @DefaultValue("5") int minimoLlamadas,
            @DefaultValue("50") float umbral,
            @DefaultValue("30s") Duration espera) {
    }
}
