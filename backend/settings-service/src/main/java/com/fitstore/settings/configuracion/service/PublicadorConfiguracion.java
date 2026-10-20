package com.fitstore.settings.configuracion.service;

import com.fitstore.common.event.OutboxRegistrador;
import com.fitstore.settings.configuracion.dto.ConfiguracionPublicaDto;
import com.fitstore.settings.configuracion.model.ConfiguracionTienda;
import com.fitstore.settings.event.ConfiguracionActualizada;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Al guardar una sección o una imagen: invalida la caché de {@code /settings/public} y registra el evento
 * {@code ConfiguracionActualizada} en el outbox, dentro de la misma transacción (ADR-009).
 */
@Component
public class PublicadorConfiguracion {

    private final ConstructorConfiguracion constructor;
    private final OutboxRegistrador outbox;
    private final CachePublica cache;
    private final JsonMapper json;

    public PublicadorConfiguracion(ConstructorConfiguracion constructor, OutboxRegistrador outbox, CachePublica cache,
            JsonMapper json) {
        this.constructor = constructor;
        this.outbox = outbox;
        this.cache = cache;
        this.json = json;
    }

    /** @param secciones IDENTIDAD, TEMA, CONTENIDO, PARAMETROS o IMAGENES */
    @SuppressWarnings("unchecked")
    public void publicar(ConfiguracionTienda configuracion, List<String> secciones) {
        ConfiguracionPublicaDto publica = constructor.publica(configuracion);
        Map<String, Object> mapa = json.convertValue(publica, Map.class);
        outbox.registrar("ConfiguracionActualizada", "configuracion", new ConfiguracionActualizada(
                configuracion.getVersion(), secciones, mapa, List.copyOf(configuracion.getParametros().getCorreosAlertas())));
        invalidarCache();
    }

    /** Invalida ya y otra vez al confirmar, para que ninguna lectura intermedia deje datos viejos en la caché. */
    public void invalidarCache() {
        cache.invalidar();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cache.invalidar();
                }
            });
        }
    }
}
