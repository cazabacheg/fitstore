package com.fitstore.catalog.catalogo.service.carga;

import com.fitstore.catalog.catalogo.service.CacheCatalogo;
import com.fitstore.catalog.catalogo.service.carga.ModeloCarga.ValidacionGuardada;
import com.fitstore.catalog.config.CatalogoProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * Guarda el resultado de validar una carga masiva durante 30 minutos en Redis (micro 4.4.1). Si Redis no responde, lo
 * guarda en la memoria del servicio para que la carga siga funcionando (ADR-020).
 */
@Component
public class AlmacenValidaciones {

    private static final String PREFIJO = "catalog:carga:";

    private final CacheCatalogo cache;
    private final Duration vigencia;
    private final Map<UUID, ValidacionGuardada> enMemoria = new ConcurrentHashMap<>();

    public AlmacenValidaciones(CacheCatalogo cache, CatalogoProperties propiedades) {
        this.cache = cache;
        this.vigencia = propiedades.cargaMasiva().vigencia();
    }

    public Duration vigencia() {
        return vigencia;
    }

    public void guardar(ValidacionGuardada validacion) {
        enMemoria.values().removeIf(v -> v.expiraEn().isBefore(Instant.now()));
        if (!cache.guardar(PREFIJO + validacion.id(), validacion, vigencia)) {
            enMemoria.put(validacion.id(), validacion);
        }
    }

    /** La validación guardada, si todavía no venció. */
    public Optional<ValidacionGuardada> obtener(UUID id) {
        Optional<ValidacionGuardada> guardada = cache.leer(PREFIJO + id, new TypeReference<ValidacionGuardada>() { });
        if (guardada.isEmpty()) {
            guardada = Optional.ofNullable(enMemoria.get(id));
        }
        return guardada.filter(v -> v.expiraEn().isAfter(Instant.now()));
    }

    public void eliminar(UUID id) {
        enMemoria.remove(id);
        cache.invalidar(PREFIJO + id);
    }
}
