package com.fitstore.payment.client;

import com.fitstore.payment.config.PropiedadesPagos;
import com.fitstore.payment.dto.ConfiguracionPublica;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Configuración pública de la tienda con caché de {@code fitstore.pagos.settings-cache-ttl} (5 min). Si settings no
 * responde se usa la última copia; si nunca respondió, devuelve vacío y quien la usa aplica su plan B
 * (proveedores con credenciales; boleta que se reintenta).
 */
@Component
public class ConfiguracionTienda {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracionTienda.class);

    private final SettingsClient cliente;
    private final Duration ttl;

    private volatile ConfiguracionPublica copia;
    private volatile Instant vence = Instant.MIN;

    public ConfiguracionTienda(SettingsClient cliente, PropiedadesPagos propiedades) {
        this.cliente = cliente;
        this.ttl = propiedades.settingsCacheTtl();
    }

    public Optional<ConfiguracionPublica> publica() {
        if (copia != null && Instant.now().isBefore(vence)) {
            return Optional.of(copia);
        }
        try {
            ConfiguracionPublica nueva = cliente.obtenerPublica().data();
            if (nueva != null) {
                copia = nueva;
                vence = Instant.now().plus(ttl);
            }
        } catch (RuntimeException e) {
            LOG.warn("No se pudo leer la configuración pública de settings: {}", e.getMessage());
        }
        return Optional.ofNullable(copia);
    }

    /** Medios habilitados en la tienda; vacío si no se pudo consultar settings. */
    public Optional<List<String>> metodosPago() {
        return publica().map(ConfiguracionPublica::parametros)
                .map(ConfiguracionPublica.Parametros::metodosPago);
    }
}
