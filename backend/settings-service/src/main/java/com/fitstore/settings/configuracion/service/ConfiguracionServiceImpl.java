package com.fitstore.settings.configuracion.service;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.settings.config.ManejadorErroresSettings;
import com.fitstore.settings.config.SolicitudInvalidaException;
import com.fitstore.settings.configuracion.dto.ConfiguracionAdminDto;
import com.fitstore.settings.configuracion.dto.ConfiguracionPublicaDto;
import com.fitstore.settings.configuracion.dto.ContenidoRequest;
import com.fitstore.settings.configuracion.dto.IdentidadRequest;
import com.fitstore.settings.configuracion.dto.ParametrosRequest;
import com.fitstore.settings.configuracion.dto.TemaRequest;
import com.fitstore.settings.configuracion.dto.TiendaFisicaRequest;
import com.fitstore.settings.configuracion.mapper.ConfiguracionMapper;
import com.fitstore.settings.configuracion.model.ConfiguracionTienda;
import com.fitstore.settings.configuracion.model.Contenido;
import com.fitstore.settings.configuracion.model.Tema;
import com.fitstore.settings.configuracion.repository.ConfiguracionTiendaRepository;
import com.fitstore.settings.configuracion.service.dominio.ContrasteWcag;
import com.fitstore.settings.ubigeo.service.UbigeoService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfiguracionServiceImpl implements ConfiguracionService {

    private static final int ID_UNICO = 1;

    private final ConfiguracionTiendaRepository repositorio;
    private final ConfiguracionMapper mapper;
    private final ConstructorConfiguracion constructor;
    private final PublicadorConfiguracion publicador;
    private final CachePublica cache;
    private final UbigeoService ubigeo;
    private final Clock reloj;

    public ConfiguracionServiceImpl(ConfiguracionTiendaRepository repositorio, ConfiguracionMapper mapper,
            ConstructorConfiguracion constructor, PublicadorConfiguracion publicador, CachePublica cache,
            UbigeoService ubigeo, Clock reloj) {
        this.repositorio = repositorio;
        this.mapper = mapper;
        this.constructor = constructor;
        this.publicador = publicador;
        this.cache = cache;
        this.ubigeo = ubigeo;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionPublicaDto obtenerPublica() {
        return cache.obtener().orElseGet(() -> {
            ConfiguracionPublicaDto publica = constructor.publica(cargar());
            cache.guardar(publica);
            return publica;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionAdminDto obtenerAdmin() {
        return constructor.admin(cargar(), List.of());
    }

    @Override
    @Transactional
    public ConfiguracionAdminDto guardarIdentidad(IdentidadRequest solicitud, String actor) {
        ConfiguracionTienda configuracion = cargarConVersion(solicitud.version());
        mapper.actualizar(solicitud, configuracion.getIdentidad());
        return guardar(configuracion, actor, "IDENTIDAD", List.of());
    }

    @Override
    @Transactional
    public ConfiguracionAdminDto guardarTema(TemaRequest solicitud, String actor) {
        ConfiguracionTienda configuracion = cargarConVersion(solicitud.version());
        mapper.actualizar(solicitud, configuracion.getTema());
        return guardar(configuracion, actor, "TEMA", advertenciasDeContraste(configuracion.getTema()));
    }

    @Override
    @Transactional
    public ConfiguracionAdminDto guardarContenido(ContenidoRequest solicitud, String actor) {
        ConfiguracionTienda configuracion = cargarConVersion(solicitud.version());
        mapper.actualizar(solicitud, configuracion.getContenido());
        asignarTiendaFisica(configuracion.getContenido(), solicitud.tiendaFisica());
        return guardar(configuracion, actor, "CONTENIDO", List.of());
    }

    @Override
    @Transactional
    public ConfiguracionAdminDto guardarParametros(ParametrosRequest solicitud, String actor) {
        ConfiguracionTienda configuracion = cargarConVersion(solicitud.version());
        mapper.actualizar(solicitud, configuracion.getParametros());
        return guardar(configuracion, actor, "PARAMETROS", List.of());
    }

    private ConfiguracionTienda cargar() {
        return repositorio.findById(ID_UNICO)
                .orElseThrow(() -> new RecursoNoEncontradoException("La tienda aún no tiene configuración"));
    }

    private ConfiguracionTienda cargarConVersion(Integer versionEnviada) {
        ConfiguracionTienda configuracion = cargar();
        if (!configuracion.getVersion().equals(versionEnviada)) {
            throw new ConflictoNegocioException("CONFLICTO_VERSION", ManejadorErroresSettings.MENSAJE_VERSION);
        }
        return configuracion;
    }

    private ConfiguracionAdminDto guardar(ConfiguracionTienda configuracion, String actor, String seccion,
            List<String> advertencias) {
        configuracion.setActualizadoEn(Instant.now(reloj));
        configuracion.setActualizadoPor(actor);
        ConfiguracionTienda guardada = repositorio.saveAndFlush(configuracion);
        publicador.publicar(guardada, List.of(seccion));
        return constructor.admin(guardada, advertencias);
    }

    private void asignarTiendaFisica(Contenido contenido, TiendaFisicaRequest tienda) {
        if (tienda == null) {
            contenido.setTiendaDireccion(null);
            contenido.setTiendaUbigeo(null);
            contenido.setTiendaReferencia(null);
            return;
        }
        if (ubigeo.nombreCompletoDeDistrito(tienda.ubigeo()).isEmpty()) {
            throw new SolicitudInvalidaException("tiendaFisica.ubigeo", "El distrito elegido no existe");
        }
        contenido.setTiendaDireccion(tienda.direccion());
        contenido.setTiendaUbigeo(tienda.ubigeo());
        contenido.setTiendaReferencia(tienda.referencia());
    }

    /** Avisa, nunca bloquea (RNF-19): texto sobre fondo y texto legible sobre el color primario. */
    private List<String> advertenciasDeContraste(Tema tema) {
        List<String> advertencias = new ArrayList<>();
        double textoFondo = ContrasteWcag.razon(tema.getColorTexto(), tema.getColorFondo());
        if (!ContrasteWcag.cumpleAA(textoFondo)) {
            advertencias.add("Contraste texto/fondo %s:1 (recomendado ≥ 4.5:1)".formatted(formatear(textoFondo)));
        }
        if (!ContrasteWcag.textoLegibleSobrePrimario(tema.getColorPrimario(), tema.getColorTexto())) {
            double mejor = Math.max(ContrasteWcag.razon(tema.getColorPrimario(), "#FFFFFF"),
                    ContrasteWcag.razon(tema.getColorPrimario(), tema.getColorTexto()));
            advertencias.add(("Sobre el color primario no se lee ni el texto blanco ni el color de texto "
                    + "(mejor contraste %s:1, recomendado ≥ 4.5:1)").formatted(formatear(mejor)));
        }
        return advertencias;
    }

    private static String formatear(double razon) {
        return String.format(Locale.ROOT, "%.1f", razon);
    }
}
