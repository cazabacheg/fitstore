package com.fitstore.order.pedidos.service;

import com.fitstore.order.client.Dependencias;
import com.fitstore.order.client.SettingsClient;
import com.fitstore.order.client.dto.ConfiguracionPublica;
import com.fitstore.order.config.PropiedadesOrder;
import com.fitstore.order.pedidos.model.Modalidad;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * Parámetros de la tienda que order necesita al confirmar (micro 7): modalidades habilitadas, minutos para pagar y
 * datos de la tienda física. Se piden a settings con un caché de 5 minutos en Redis; si settings no responde y no hay
 * copia, se usan los valores por defecto (todas las modalidades y 30 minutos).
 */
@Service
public class ParametrosTienda {

    static final String CLAVE_CACHE = "order:settings:public";
    private static final Logger LOG = LoggerFactory.getLogger(ParametrosTienda.class);

    /** Lo que order usa de la configuración pública; {@code tiendaDireccion} es nula si settings no la tiene. */
    public record Parametros(List<Modalidad> modalidades, int expiracionMinutos, String tiendaDireccion) {
    }

    private final SettingsClient settings;
    private final Dependencias dependencias;
    private final StringRedisTemplate redis;
    private final PropiedadesOrder propiedades;
    private final JsonMapper json;

    public ParametrosTienda(SettingsClient settings, Dependencias dependencias, StringRedisTemplate redis,
            PropiedadesOrder propiedades, JsonMapper json) {
        this.settings = settings;
        this.dependencias = dependencias;
        this.redis = redis;
        this.propiedades = propiedades;
        this.json = json;
    }

    public Parametros obtener() {
        Optional<Parametros> enCache = leerCache();
        if (enCache.isPresent()) {
            return enCache.get();
        }
        try {
            var respuesta = dependencias.llamar(settings::configuracionPublica);
            Parametros parametros = convertir(respuesta.data());
            redis.opsForValue().set(CLAVE_CACHE, json.writeValueAsString(parametros), propiedades.settingsCacheTtl());
            return parametros;
        } catch (RuntimeException e) {
            LOG.warn("Settings no respondió; se usan los parámetros por defecto: {}", e.getMessage());
            return new Parametros(List.of(Modalidad.values()), propiedades.defaults().expiracionPedidoMinutos(), null);
        }
    }

    private Optional<Parametros> leerCache() {
        try {
            String texto = redis.opsForValue().get(CLAVE_CACHE);
            return texto == null ? Optional.empty() : Optional.of(json.readValue(texto, Parametros.class));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static Parametros convertir(ConfiguracionPublica configuracion) {
        List<Modalidad> modalidades = Arrays.stream(Modalidad.values())
                .filter(m -> configuracion.parametros().modalidadesEntrega().contains(m.name())).toList();
        return new Parametros(modalidades, configuracion.parametros().expiracionPedidoMinutos(),
                direccionDeLaTienda(configuracion.contenido()));
    }

    private static String direccionDeLaTienda(ConfiguracionPublica.Contenido contenido) {
        if (contenido == null || contenido.tiendaFisica() == null) {
            return null;
        }
        var tienda = contenido.tiendaFisica();
        StringBuilder texto = new StringBuilder(tienda.direccion());
        if (tienda.ubigeoNombre() != null) {
            texto.append(", ").append(tienda.ubigeoNombre());
        }
        if (tienda.referencia() != null && !tienda.referencia().isBlank()) {
            texto.append(" (").append(tienda.referencia()).append(")");
        }
        if (contenido.horario() != null && !contenido.horario().isBlank()) {
            texto.append(" · ").append(contenido.horario());
        }
        return texto.toString();
    }
}
