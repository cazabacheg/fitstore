package com.fitstore.notification.plantillas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitstore.notification.client.SettingsClient;
import com.fitstore.notification.messaging.EnrutadorEventos;
import com.fitstore.notification.model.MarcaTienda;
import com.fitstore.notification.pruebas.EventosDePrueba;
import com.fitstore.notification.pruebas.LimpiezaBd;
import com.fitstore.notification.pruebas.PostgresInicializador;
import com.fitstore.notification.repository.MarcaTiendaRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** La copia local de la marca: se actualiza con cada ConfiguracionActualizada y se pide una vez al arrancar sin copia. */
@SpringBootTest
@ActiveProfiles({"test", "sin-config-server"})
@ContextConfiguration(initializers = PostgresInicializador.class)
class MarcaTiendaTest {

    @Autowired MarcaService marca;
    @Autowired EnrutadorEventos enrutador;
    @Autowired MarcaTiendaRepository repositorio;
    @Autowired RenderizadorCorreo renderizador;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean SettingsClient settings;

    @BeforeEach
    void preparar() {
        LimpiezaBd.limpiar(jdbc);
        reset(settings);
    }

    /** Las pruebas comparten un solo PostgreSQL con las de repositorio, que esperan la base recién migrada. */
    @AfterEach
    void dejarLaBaseComoLaMigracion() {
        LimpiezaBd.limpiar(jdbc);
    }

    private static Map<String, Object> configuracion(String nombre, String primario, String logo) {
        return Map.of(
                "identidad", Map.of("nombre", nombre, "logoClaroUrl", logo),
                "tema", Map.of("colorPrimario", primario, "colorSecundario", "#111111", "colorAcento", "#222222",
                        "colorFondo", "#FFFFFF", "colorTexto", "#000000"),
                "contenido", Map.of("email", "hola@nueva.test", "telefono", "999888777", "horario", "Todos los días",
                        "instagramUrl", "https://instagram.com/nueva",
                        "tiendaFisica", Map.of("direccion", "Av. Nueva 123")));
    }

    private void configuracionActualizada(int version, Map<String, Object> configuracion, List<String> alertas) {
        enrutador.procesar(EventosDePrueba.sobre("ConfiguracionActualizada", Map.of(
                "version", version, "secciones", List.of("TEMA"), "configuracion", configuracion,
                "correosAlertas", alertas)));
    }

    @Test
    @DisplayName("CA-7-16 ConfiguracionActualizada actualiza la copia y los correos siguientes usan los nuevos colores, logo y datos")
    void actualizaLaCopia() {
        configuracionActualizada(5, configuracion("Nueva Marca", "#AA3355", "http://tienda.test/media/nueva.png"),
                List.of("uno@nueva.test", "dos@nueva.test"));

        MarcaVista actual = marca.actual();
        assertThat(actual.nombre()).isEqualTo("Nueva Marca");
        assertThat(actual.colorPrimario()).isEqualTo("#AA3355");
        assertThat(actual.logoUrl()).isEqualTo("http://tienda.test/media/nueva.png");
        assertThat(actual.email()).isEqualTo("hola@nueva.test");
        assertThat(actual.direccion()).isEqualTo("Av. Nueva 123");
        MarcaTienda fila = repositorio.findById((short) 1).orElseThrow();
        assertThat(fila.getVersion()).isEqualTo(5);
        assertThat(fila.getCorreosAlertas()).containsExactly("uno@nueva.test", "dos@nueva.test");
        CorreoRenderizado correo = renderizador.renderizar(CatalogoPlantillas.BIENVENIDA,
                new TextosPlantilla("Hola", "Bienvenida", "Hola {{nombre}}", null),
                EjemplosPlantilla.de(CatalogoPlantillas.BIENVENIDA), actual);
        assertThat(correo.html()).contains("#AA3355").contains("http://tienda.test/media/nueva.png")
                .contains("Av. Nueva 123").contains("Nueva Marca");
    }

    @Test
    @DisplayName("CA-7-16 una configuración más vieja que la copia no la pisa")
    void ignoraVersionesViejas() {
        configuracionActualizada(7, configuracion("Marca v7", "#AA3355", "http://tienda.test/media/v7.png"), List.of("a@x.test"));

        configuracionActualizada(6, configuracion("Marca v6", "#00FF00", "http://tienda.test/media/v6.png"), List.of("b@x.test"));

        assertThat(marca.actual().nombre()).isEqualTo("Marca v7");
        assertThat(repositorio.findById((short) 1).orElseThrow().getVersion()).isEqualTo(7);
    }

    @Test
    @DisplayName("CA-7-17 sin copia de la marca al arrancar se pide una vez GET /settings/public y se guarda")
    void arrancaSinCopia() {
        jdbc.update("DELETE FROM marca_tienda");
        when(settings.obtenerPublica()).thenReturn(configuracion("Desde Settings", "#336699", "http://tienda.test/media/s.png"));

        marca.cargarAlArrancar();
        marca.cargarAlArrancar();

        verify(settings, times(1)).obtenerPublica();
        assertThat(marca.actual().nombre()).isEqualTo("Desde Settings");
        assertThat(marca.actual().colorPrimario()).isEqualTo("#336699");
        assertThat(repositorio.findById((short) 1)).isPresent();
    }

    @Test
    @DisplayName("CA-7-17 si settings falla al arrancar se usa la marca FitStore por defecto")
    void arrancaSinCopiaYSettingsCaido() {
        jdbc.update("DELETE FROM marca_tienda");
        when(settings.obtenerPublica()).thenThrow(new IllegalStateException("settings no responde"));

        marca.cargarAlArrancar();

        MarcaVista actual = marca.actual();
        assertThat(actual.nombre()).isEqualTo("FitStore");
        assertThat(actual.colorPrimario()).isEqualTo("#C9B28C");
        assertThat(actual.colorTexto()).isEqualTo("#26221D");
        assertThat(marca.correosAlertas()).isNotNull();
    }
}
