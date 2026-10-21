package com.fitstore.delivery.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración propia de delivery ({@code fitstore.delivery}, micro 10.5).
 *
 * @param local zona del delivery local: solo Lima Metropolitana y Callao
 */
@ConfigurationProperties(prefix = "fitstore.delivery")
public record PropiedadesDelivery(Local local) {

    public PropiedadesDelivery {
        local = local == null ? new Local(null) : local;
    }

    /** @param prefijosUbigeo prefijos de ubigeo que cubre el delivery local (por defecto {@code 1501} y {@code 07}) */
    public record Local(List<String> prefijosUbigeo) {

        public Local {
            prefijosUbigeo = prefijosUbigeo == null || prefijosUbigeo.isEmpty()
                    ? List.of("1501", "07") : List.copyOf(prefijosUbigeo);
        }
    }

    /** ¿El delivery local llega a este ubigeo (de 2, 4 o 6 dígitos)? */
    public boolean esZonaLocal(String ubigeo) {
        return local.prefijosUbigeo().stream().anyMatch(ubigeo::startsWith);
    }
}
