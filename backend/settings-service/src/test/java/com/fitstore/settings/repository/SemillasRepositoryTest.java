package com.fitstore.settings.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.settings.configuracion.model.ConfiguracionTienda;
import com.fitstore.settings.configuracion.model.MetodoPago;
import com.fitstore.settings.configuracion.model.ModalidadEntrega;
import com.fitstore.settings.configuracion.model.Politica;
import com.fitstore.settings.configuracion.model.TipoPolitica;
import com.fitstore.settings.configuracion.repository.ConfiguracionTiendaRepository;
import com.fitstore.settings.configuracion.repository.PoliticaRepository;
import com.fitstore.settings.pruebas.PruebaDatos;
import com.fitstore.settings.ubigeo.repository.DepartamentoRepository;
import com.fitstore.settings.ubigeo.repository.DistritoRepository;
import com.fitstore.settings.ubigeo.repository.ProvinciaRepository;
import com.fitstore.settings.ubigeo.repository.UbigeoRepository;
import com.fitstore.settings.ubigeo.repository.UbigeoRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/** Las migraciones V2 (ubigeo del INEI) y V3 (configuración inicial) dejan los datos esperados. */
@Import(UbigeoRepositoryImpl.class)
class SemillasRepositoryTest extends PruebaDatos {

    @Autowired DepartamentoRepository departamentos;
    @Autowired ProvinciaRepository provincias;
    @Autowired DistritoRepository distritos;
    @Autowired UbigeoRepository ubigeo;
    @Autowired ConfiguracionTiendaRepository configuracion;
    @Autowired PoliticaRepository politicas;

    @Test
    @DisplayName("CA-0a-06 V2 carga los conteos del INEI: 25 departamentos, 196 provincias y 1 893 distritos")
    void conteosDelInei() {
        assertThat(departamentos.count()).isEqualTo(25);
        assertThat(provincias.count()).isEqualTo(196);
        assertThat(distritos.count()).isEqualTo(1893);
    }

    @Test
    @DisplayName("CA-0a-06 V2: 150122 es Miraflores y 150115 es La Victoria, de Lima, Lima (códigos INEI)")
    void codigosConocidos() {
        assertThat(ubigeo.nombreCompleto("150122")).contains("MIRAFLORES, LIMA, LIMA");
        assertThat(ubigeo.nombreCompleto("150115")).contains("LA VICTORIA, LIMA, LIMA");
        assertThat(ubigeo.nombreCompleto("180107")).contains("SAN ANTONIO, MARISCAL NIETO, MOQUEGUA");
    }

    @Test
    @DisplayName("CA-0a-06 V2: Lima tiene 10 provincias y Lima (provincia) tiene 44 distritos")
    void jerarquiaDeLima() {
        assertThat(ubigeo.provinciasDe("15")).hasSize(10);
        assertThat(ubigeo.distritosDe("1501")).hasSize(44);
    }

    @Test
    @DisplayName("CA-0a-06 V2: el buscador encuentra distritos sobre el maestro real sin importar tildes")
    void buscadorSobreElMaestro() {
        assertThat(ubigeo.buscarDistritos("miraflores")).extracting(d -> d.getCodigo()).contains("150122", "040110");
        assertThat(ubigeo.buscarDistritos("Unión Ashaninka")).isNotEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 V3: la configuración inicial es la de la marca de demostración")
    void configuracionSemilla() {
        ConfiguracionTienda c = configuracion.findById(1).orElseThrow();

        assertThat(c.getIdentidad().getNombre()).isEqualTo("FitStore");
        assertThat(c.getIdentidad().getEslogan()).isEqualTo("Ropa deportiva hecha en Gamarra.");
        assertThat(c.getIdentidad().getRazonSocial()).isEqualTo("Moda Activa S.A.C.");
        assertThat(c.getIdentidad().getRuc()).isEqualTo("20123456789");
        assertThat(c.getIdentidad().getDireccionFiscal()).isEqualTo("Jr. Gamarra 700, tienda 210, La Victoria, Lima");

        assertThat(c.getTema().getColorPrimario()).isEqualTo("#C9B28C");
        assertThat(c.getTema().getColorSecundario()).isEqualTo("#2B2621");
        assertThat(c.getTema().getColorAcento()).isEqualTo("#4E5B45");
        assertThat(c.getTema().getColorFondo()).isEqualTo("#FAF9F7");
        assertThat(c.getTema().getColorTexto()).isEqualTo("#26221D");

        assertThat(c.getContenido().getHeroTitulo()).isEqualTo("Muévete con estilo");
        assertThat(c.getContenido().getHeroSubtitulo())
                .isEqualTo("Leggings, tops y casacas hechos en Gamarra, con tela de compresión y secado rápido.");
        assertThat(c.getContenido().getHeroCtaTexto()).isEqualTo("Ver novedades");
        assertThat(c.getContenido().getHeroCtaEnlace()).isEqualTo("/productos");
        assertThat(c.getContenido().getBeneficios()).isEmpty();
        assertThat(c.getContenido().getEmail()).isEqualTo("ventas@tienda.pe");
        assertThat(c.getContenido().getTelefono()).isEqualTo("987654321");
        assertThat(c.getContenido().getWhatsapp()).isEqualTo("987654321");
        assertThat(c.getContenido().getHorario()).isEqualTo("Lunes a sábado de 9:00 a 19:00");
        assertThat(c.getContenido().getInstagramUrl()).isNull();
        assertThat(c.getContenido().getFacebookUrl()).isNull();
        assertThat(c.getContenido().getTiktokUrl()).isNull();
        assertThat(c.getContenido().getTiendaDireccion()).isEqualTo("Galería Gamarra, Jr. Gamarra 700, tienda 210");
        assertThat(c.getContenido().getTiendaUbigeo()).isEqualTo("150115");
        assertThat(c.getContenido().getTiendaReferencia()).isNull();

        assertThat(c.getParametros().getMetodosPago()).containsExactlyInAnyOrder(MetodoPago.values());
        assertThat(c.getParametros().getModalidadesEntrega()).containsExactlyInAnyOrder(ModalidadEntrega.values());
        assertThat(c.getParametros().getExpiracionPedidoMinutos()).isEqualTo(30);
        assertThat(c.getParametros().getDiasReembolsoPostventa()).isEqualTo(30);
        assertThat(c.getParametros().getCorreosAlertas()).containsExactly("ventas@tienda.pe");
        assertThat(c.getVersion()).isZero();
        assertThat(configuracion.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-0a-06 V3: la ubicación de la tienda física existe en el ubigeo")
    void tiendaFisicaEnElUbigeo() {
        String ubigeoTienda = configuracion.findById(1).orElseThrow().getContenido().getTiendaUbigeo();

        assertThat(ubigeo.nombreCompleto(ubigeoTienda)).contains("LA VICTORIA, LIMA, LIMA");
    }

    @ParameterizedTest
    @EnumSource(TipoPolitica.class)
    @DisplayName("CA-0a-06 V3: cada política tiene su versión 1 (SPEC-3 reemplazó el ejemplo por el texto modelo)")
    void politicasIniciales(TipoPolitica tipo) {
        Politica p = politicas.findFirstByTipoOrderByVersionDesc(tipo).orElseThrow();

        assertThat(p.getVersion()).isEqualTo(1);
        // V3 la creó con un texto de ejemplo; V20261020_1100 (SPEC-3) lo reemplaza por la plantilla a revisar
        assertThat(p.getContenido()).contains("Plantilla a revisar por la tienda");
        assertThat(politicas.findByTipoOrderByVersionDesc(tipo)).hasSize(1);
    }
}
