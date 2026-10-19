package com.fitstore.gateway.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración propia del gateway ({@code fitstore.gateway.*}, en {@code api-gateway.yml}).
 *
 * @param rateLimit límites de peticiones (infraestructura 4.4, ADR-020)
 * @param cors      orígenes permitidos (storefront y backoffice)
 */
@ConfigurationProperties(prefix = "fitstore.gateway")
public record GatewayProperties(RateLimit rateLimit, Cors cors) {

    public GatewayProperties {
        rateLimit = rateLimit == null ? new RateLimit(null, null) : rateLimit;
        cors = cors == null ? new Cors(null, null, null, null) : cors;
    }

    /**
     * @param reglas reglas específicas por ruta; se aplica la primera que coincide con el método y la ruta
     * @param resto  límite de las demás rutas
     */
    public record RateLimit(List<Regla> reglas, Resto resto) {
        public RateLimit {
            reglas = reglas == null ? List.of() : List.copyOf(reglas);
            resto = resto == null ? new Resto(50, 100) : resto;
        }
    }

    /**
     * @param nombre           identifica la regla en la clave de Redis
     * @param metodo           método HTTP
     * @param rutas            patrones de ruta (con el prefijo {@code /api/v1})
     * @param limite           peticiones permitidas por ventana
     * @param ventanaSegundos  duración de la ventana
     * @param clave            {@code IP} o {@code USUARIO} (el {@code sub} del JWT; sin token cae a la IP)
     */
    public record Regla(String nombre, String metodo, List<String> rutas, int limite, int ventanaSegundos, Clave clave) {
        public Regla {
            rutas = rutas == null ? List.of() : List.copyOf(rutas);
            clave = clave == null ? Clave.IP : clave;
        }
    }

    /**
     * Token bucket por IP para el resto de las rutas: "50 por segundo (ráfaga 100)" es
     * {@code reposicionPorSegundo = 50} y {@code capacidad = 100}.
     */
    public record Resto(int reposicionPorSegundo, int capacidad) {
    }

    public enum Clave {
        IP,
        USUARIO
    }

    /** Orígenes (storefront y backoffice) y qué se les permite. */
    public record Cors(List<String> origenes, List<String> metodos, List<String> cabeceras, Long maxAgeSegundos) {
        public Cors {
            origenes = origenes == null ? List.of() : List.copyOf(origenes);
            metodos = metodos == null ? List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") : List.copyOf(metodos);
            cabeceras = cabeceras == null
                    ? List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Carrito-Id", "X-Request-Id")
                    : List.copyOf(cabeceras);
            maxAgeSegundos = maxAgeSegundos == null ? 3600L : maxAgeSegundos;
        }
    }
}
