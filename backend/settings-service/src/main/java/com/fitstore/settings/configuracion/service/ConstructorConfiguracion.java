package com.fitstore.settings.configuracion.service;

import com.fitstore.common.storage.FileStorage;
import com.fitstore.settings.configuracion.dto.ConfiguracionAdminDto;
import com.fitstore.settings.configuracion.dto.ConfiguracionPublicaDto;
import com.fitstore.settings.configuracion.dto.ContenidoDto;
import com.fitstore.settings.configuracion.dto.IdentidadDto;
import com.fitstore.settings.configuracion.dto.ImagenTiendaDto;
import com.fitstore.settings.configuracion.dto.ParametrosPublicosDto;
import com.fitstore.settings.configuracion.dto.TiendaFisicaDto;
import com.fitstore.settings.configuracion.dto.VersionesPoliticasDto;
import com.fitstore.settings.configuracion.mapper.ConfiguracionMapper;
import com.fitstore.settings.configuracion.model.ConfiguracionTienda;
import com.fitstore.settings.configuracion.model.Contenido;
import com.fitstore.settings.configuracion.model.Identidad;
import com.fitstore.settings.configuracion.model.ImagenTienda;
import com.fitstore.settings.configuracion.model.Parametros;
import com.fitstore.settings.configuracion.model.TipoImagen;
import com.fitstore.settings.configuracion.model.TipoPolitica;
import com.fitstore.settings.configuracion.repository.ImagenTiendaRepository;
import com.fitstore.settings.configuracion.repository.PoliticaRepository;
import com.fitstore.settings.ubigeo.service.UbigeoService;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Arma la configuración (pública o completa) a partir de la fila de configuración, las imágenes y las políticas. */
@Component
public class ConstructorConfiguracion {

    private final ImagenTiendaRepository imagenes;
    private final PoliticaRepository politicas;
    private final UbigeoService ubigeo;
    private final FileStorage almacen;
    private final ConfiguracionMapper mapper;

    public ConstructorConfiguracion(ImagenTiendaRepository imagenes, PoliticaRepository politicas,
            UbigeoService ubigeo, FileStorage almacen, ConfiguracionMapper mapper) {
        this.imagenes = imagenes;
        this.politicas = politicas;
        this.ubigeo = ubigeo;
        this.almacen = almacen;
        this.mapper = mapper;
    }

    public ConfiguracionPublicaDto publica(ConfiguracionTienda c) {
        return new ConfiguracionPublicaDto(identidad(c.getIdentidad()), mapper.aDto(c.getTema()),
                contenido(c.getContenido()), parametros(c.getParametros()), versionesDePoliticas());
    }

    public ConfiguracionAdminDto admin(ConfiguracionTienda c, List<String> advertencias) {
        ConfiguracionPublicaDto p = publica(c);
        return new ConfiguracionAdminDto(p.identidad(), p.tema(), p.contenido(), p.parametros(), p.politicas(),
                List.copyOf(c.getParametros().getCorreosAlertas()), c.getVersion(), c.getActualizadoEn(),
                c.getActualizadoPor(), advertencias);
    }

    public ImagenTiendaDto imagen(ImagenTienda i) {
        return new ImagenTiendaDto(i.getId(), i.getTipo(), almacen.urlPublica(i.getClave()), i.getOrden(),
                i.getTextoAlternativo(), i.getEnlace());
    }

    private IdentidadDto identidad(Identidad i) {
        return new IdentidadDto(i.getNombre(), i.getEslogan(), i.getRazonSocial(), i.getRuc(), i.getDireccionFiscal(),
                urlDe(TipoImagen.LOGO_CLARO), urlDe(TipoImagen.LOGO_OSCURO), urlDe(TipoImagen.FAVICON));
    }

    private String urlDe(TipoImagen tipo) {
        return imagenes.findByTipoOrderByOrdenAsc(tipo).stream().findFirst()
                .map(i -> almacen.urlPublica(i.getClave())).orElse(null);
    }

    private ContenidoDto contenido(Contenido c) {
        List<ImagenTiendaDto> landing = imagenes.findByTipoOrderByOrdenAsc(TipoImagen.LANDING).stream()
                .sorted(Comparator.comparingInt(ImagenTienda::getOrden).thenComparing(ImagenTienda::getId))
                .map(this::imagen).toList();
        return new ContenidoDto(c.getHeroTitulo(), c.getHeroSubtitulo(), c.getHeroCtaTexto(), c.getHeroCtaEnlace(),
                mapper.aDtos(c.getBeneficios()), c.getEmail(), c.getTelefono(), c.getWhatsapp(), c.getInstagramUrl(),
                c.getFacebookUrl(), c.getTiktokUrl(), c.getHorario(), tiendaFisica(c), landing);
    }

    private TiendaFisicaDto tiendaFisica(Contenido c) {
        if (c.getTiendaDireccion() == null || c.getTiendaUbigeo() == null) {
            return null;
        }
        String nombre = ubigeo.nombreCompletoDeDistrito(c.getTiendaUbigeo()).orElse(null);
        return new TiendaFisicaDto(c.getTiendaDireccion(), c.getTiendaUbigeo(), nombre, c.getTiendaReferencia());
    }

    private ParametrosPublicosDto parametros(Parametros p) {
        return new ParametrosPublicosDto(p.getMetodosPago().stream().sorted().toList(),
                p.getModalidadesEntrega().stream().sorted().toList(), p.getExpiracionPedidoMinutos(),
                p.getDiasReembolsoPostventa(), ParametrosPublicosDto.MONEDA);
    }

    private VersionesPoliticasDto versionesDePoliticas() {
        return new VersionesPoliticasDto(vigente(TipoPolitica.TERMINOS), vigente(TipoPolitica.PRIVACIDAD),
                vigente(TipoPolitica.CAMBIOS_DEVOLUCIONES), vigente(TipoPolitica.ENVIOS));
    }

    private int vigente(TipoPolitica tipo) {
        return Optional.ofNullable(politicas.findFirstByTipoOrderByVersionDesc(tipo).orElse(null))
                .map(p -> p.getVersion()).orElse(1);
    }
}
