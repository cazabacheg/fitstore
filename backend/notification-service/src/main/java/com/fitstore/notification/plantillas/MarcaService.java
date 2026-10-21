package com.fitstore.notification.plantillas;

import com.fitstore.notification.client.SettingsClient;
import com.fitstore.notification.config.PropiedadesNotificacion;
import com.fitstore.notification.messaging.evento.ConfiguracionActualizada;
import com.fitstore.notification.model.MarcaTienda;
import com.fitstore.notification.repository.MarcaTiendaRepository;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copia local de la marca de la tienda (micro D3): se actualiza con cada {@code ConfiguracionActualizada}; si al
 * arrancar no hay copia real se pide una vez {@code GET /settings/public} y, si falla, se usa la marca FitStore.
 */
@Service
public class MarcaService {

    private static final Logger LOG = LoggerFactory.getLogger(MarcaService.class);
    private static final short ID = 1;

    private final MarcaTiendaRepository repositorio;
    private final SettingsClient settings;
    private final PropiedadesNotificacion propiedades;
    private final Clock reloj;

    public MarcaService(MarcaTiendaRepository repositorio, SettingsClient settings, PropiedadesNotificacion propiedades,
            Clock reloj) {
        this.repositorio = repositorio;
        this.settings = settings;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    /** La marca vigente para armar un correo. */
    @Transactional(readOnly = true)
    public MarcaVista actual() {
        String tienda = propiedades.enlaces().storefront();
        return repositorio.findById(ID).map(m -> MarcaVista.de(m.getConfiguracion(), tienda))
                .orElseGet(() -> MarcaVista.porDefecto(tienda));
    }

    /** Los "correos para alertas" de la tienda (D5); vacío si todavía no hay copia. */
    @Transactional(readOnly = true)
    public List<String> correosAlertas() {
        return repositorio.findById(ID).map(m -> Arrays.asList(m.getCorreosAlertas())).orElse(List.of());
    }

    /** Guarda la configuración del evento, salvo que sea más vieja que la copia (llegó desordenada). */
    @Transactional
    public void actualizar(ConfiguracionActualizada evento) {
        MarcaTienda marca = repositorio.findById(ID).orElseGet(() -> {
            MarcaTienda nueva = new MarcaTienda();
            nueva.setId(ID);
            nueva.setVersion(-1);
            return nueva;
        });
        if (evento.version() < marca.getVersion()) {
            LOG.info("Se ignora ConfiguracionActualizada v{}: la copia ya está en v{}", evento.version(), marca.getVersion());
            return;
        }
        marca.setConfiguracion(evento.configuracion());
        marca.setCorreosAlertas(evento.correosAlertas() == null ? new String[0] : evento.correosAlertas().toArray(String[]::new));
        marca.setVersion(evento.version());
        marca.setActualizadoEn(reloj.instant());
        repositorio.save(marca);
    }

    /** Al arrancar: si la copia es la semilla (versión 0) o no existe, la pide a settings; si falla, queda como está. */
    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        cargarAlArrancar();
    }

    @Transactional
    public void cargarAlArrancar() {
        MarcaTienda existente = repositorio.findById(ID).orElse(null);
        if (existente != null && existente.getVersion() > 0) {
            return;
        }
        try {
            Map<String, Object> publica = settings.obtenerPublica();
            if (publica == null) {
                throw new IllegalStateException("settings respondió vacío");
            }
            MarcaTienda marca = existente != null ? existente : new MarcaTienda();
            marca.setId(ID);
            marca.setConfiguracion(publica);
            marca.setCorreosAlertas(existente != null ? existente.getCorreosAlertas() : new String[0]);
            marca.setVersion(1);
            marca.setActualizadoEn(reloj.instant());
            repositorio.save(marca);
            LOG.info("Marca de la tienda copiada de settings al arrancar");
        } catch (RuntimeException e) {
            LOG.warn("No se pudo pedir la marca a settings; se usa {}: {}",
                    existente != null ? "la copia sembrada" : "la marca FitStore por defecto", e.getMessage());
        }
    }
}
