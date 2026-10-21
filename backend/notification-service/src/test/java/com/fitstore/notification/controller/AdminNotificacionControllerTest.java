package com.fitstore.notification.controller;

import static com.fitstore.notification.pruebas.Sesiones.admin;
import static com.fitstore.notification.pruebas.Sesiones.operador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.notification.model.EnvioCorreo;
import com.fitstore.notification.model.EstadoEnvio;
import com.fitstore.notification.pruebas.LimpiezaBd;
import com.fitstore.notification.pruebas.PostgresInicializador;
import com.fitstore.notification.repository.EnvioCorreoRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/** Registro de envíos, detalle, reenvío y permisos de la API del backoffice (SPEC-7 CA-7-19 a CA-7-22). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "sin-config-server"})
@ContextConfiguration(initializers = PostgresInicializador.class)
class AdminNotificacionControllerTest {

    private static final String BASE = "/api/v1/admin/notificaciones";
    private static final Instant AHORA = Instant.parse("2026-10-22T15:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired EnvioCorreoRepository envios;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void preparar() {
        LimpiezaBd.limpiar(jdbc);
    }

    /** Las pruebas comparten un solo PostgreSQL con las de repositorio, que esperan la base recién migrada. */
    @AfterEach
    void dejarLaBaseComoLaMigracion() {
        LimpiezaBd.limpiar(jdbc);
    }

    private EnvioCorreo envio(String plantilla, String destinatario, String referencia, EstadoEnvio estado, Instant creadoEn) {
        EnvioCorreo e = new EnvioCorreo();
        e.setId(UUID.randomUUID());
        e.setEventoId(UUID.randomUUID());
        e.setPlantilla(plantilla);
        e.setDestinatario(destinatario);
        e.setAsunto("Asunto de " + plantilla + " " + referencia);
        e.setVariables(Map.of("nombre", "Ana"));
        e.setEstado(estado);
        e.setIntentos(estado == EstadoEnvio.PENDIENTE ? 0 : 1);
        e.setHistorialIntentos(new ArrayList<>());
        e.setReferencia(referencia);
        e.setCreadoEn(creadoEn);
        if (estado == EstadoEnvio.PENDIENTE) {
            e.setProximoIntentoEn(creadoEn);
        }
        if (estado == EstadoEnvio.ENVIADO) {
            e.setEnviadoEn(creadoEn.plusSeconds(3));
            e.setHistorialIntentos(new ArrayList<>(List.of(intento(1, creadoEn.plusSeconds(3), null))));
        }
        if (estado == EstadoEnvio.FALLIDO) {
            e.setUltimoError("Connection refused");
            e.setHistorialIntentos(new ArrayList<>(List.of(intento(1, creadoEn.plusSeconds(3), "Connection refused"))));
        }
        return envios.saveAndFlush(e);
    }

    private static Map<String, Object> intento(int numero, Instant fecha, String error) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("numero", numero);
        m.put("fecha", fecha.toString());
        m.put("error", error);
        return m;
    }

    @Test
    @DisplayName("CA-7-19 lista del más reciente al más antiguo con los datos de la tabla y los contadores por estado")
    void listaOrdenadaConContadores() throws Exception {
        envio("BIENVENIDA", "ana@fitstore.test", null, EstadoEnvio.ENVIADO, AHORA.minusSeconds(300));
        envio("PEDIDO_CONFIRMADO", "ana@fitstore.test", "FS-000101", EstadoEnvio.FALLIDO, AHORA.minusSeconds(100));
        envio("EN_CAMINO", "luis@fitstore.test", "FS-000102", EstadoEnvio.PENDIENTE, AHORA.minusSeconds(10));

        mvc.perform(get(BASE).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.contenido", hasSize(3)))
                .andExpect(jsonPath("$.data.contenido[*].plantilla", contains("EN_CAMINO", "PEDIDO_CONFIRMADO", "BIENVENIDA")))
                .andExpect(jsonPath("$.data.contenido[0].destinatario", is("luis@fitstore.test")))
                .andExpect(jsonPath("$.data.contenido[0].referencia", is("FS-000102")))
                .andExpect(jsonPath("$.data.contenido[0].estado", is("PENDIENTE")))
                .andExpect(jsonPath("$.data.contenido[1].intentos", is(1)))
                .andExpect(jsonPath("$.data.contenido[2].referencia", is(nullValue())))
                .andExpect(jsonPath("$.data.totalElementos", is(3)))
                .andExpect(jsonPath("$.data.pagina", is(0)))
                .andExpect(jsonPath("$.data.contadores.PENDIENTE", is(1)))
                .andExpect(jsonPath("$.data.contadores.ENVIADO", is(1)))
                .andExpect(jsonPath("$.data.contadores.FALLIDO", is(1)));
    }

    @Test
    @DisplayName("CA-7-19 filtra por estado, plantilla, destinatario, referencia y fechas, y pagina")
    void filtros() throws Exception {
        envio("BIENVENIDA", "ana@fitstore.test", null, EstadoEnvio.ENVIADO, Instant.parse("2026-10-20T12:00:00Z"));
        envio("PEDIDO_CONFIRMADO", "ana@fitstore.test", "FS-000101", EstadoEnvio.FALLIDO, Instant.parse("2026-10-21T12:00:00Z"));
        envio("EN_CAMINO", "luis@fitstore.test", "FS-000102", EstadoEnvio.PENDIENTE, Instant.parse("2026-10-22T12:00:00Z"));

        mvc.perform(get(BASE).param("estado", "FALLIDO").with(admin()))
                .andExpect(jsonPath("$.data.contenido", hasSize(1)))
                .andExpect(jsonPath("$.data.contenido[0].plantilla", is("PEDIDO_CONFIRMADO")))
                // los contadores no aplican el filtro de estado
                .andExpect(jsonPath("$.data.contadores.ENVIADO", is(1)));
        mvc.perform(get(BASE).param("plantilla", "EN_CAMINO").with(admin()))
                .andExpect(jsonPath("$.data.contenido[*].referencia", contains("FS-000102")));
        mvc.perform(get(BASE).param("destinatario", "ana@").with(admin()))
                .andExpect(jsonPath("$.data.contenido", hasSize(2)));
        mvc.perform(get(BASE).param("referencia", "FS-000101").with(admin()))
                .andExpect(jsonPath("$.data.contenido", hasSize(1)))
                .andExpect(jsonPath("$.data.contenido[0].estado", is("FALLIDO")));
        mvc.perform(get(BASE).param("desde", "2026-10-21").param("hasta", "2026-10-21").with(admin()))
                .andExpect(jsonPath("$.data.contenido", hasSize(1)))
                .andExpect(jsonPath("$.data.contenido[0].plantilla", is("PEDIDO_CONFIRMADO")));
        mvc.perform(get(BASE).param("size", "2").param("page", "1").with(admin()))
                .andExpect(jsonPath("$.data.contenido", hasSize(1)))
                .andExpect(jsonPath("$.data.totalElementos", is(3)))
                .andExpect(jsonPath("$.data.totalPaginas", is(2)))
                .andExpect(jsonPath("$.data.contenido[0].plantilla", is("BIENVENIDA")));
    }

    @Test
    @DisplayName("CA-7-20 el detalle trae asunto, destinatario, referencia, cada intento con su fecha y el último error")
    void detalle() throws Exception {
        EnvioCorreo fallido = envio("PEDIDO_CONFIRMADO", "ana@fitstore.test", "FS-000101", EstadoEnvio.FALLIDO, AHORA);

        mvc.perform(get(BASE + "/" + fallido.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(fallido.getId().toString())))
                .andExpect(jsonPath("$.data.asunto", is("Asunto de PEDIDO_CONFIRMADO FS-000101")))
                .andExpect(jsonPath("$.data.destinatario", is("ana@fitstore.test")))
                .andExpect(jsonPath("$.data.referencia", is("FS-000101")))
                .andExpect(jsonPath("$.data.ultimoError", is("Connection refused")))
                .andExpect(jsonPath("$.data.historialIntentos", hasSize(1)))
                .andExpect(jsonPath("$.data.historialIntentos[0].numero", is(1)))
                .andExpect(jsonPath("$.data.historialIntentos[0].error", is("Connection refused")))
                .andExpect(jsonPath("$.data.historialIntentos[0].fecha").exists());
        mvc.perform(get(BASE + "/" + UUID.randomUUID()).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-7-21 reenviar un envío FALLIDO o ENVIADO lo vuelve a PENDIENTE para que el despachador lo envíe")
    void reenviar() throws Exception {
        EnvioCorreo fallido = envio("PEDIDO_CONFIRMADO", "ana@fitstore.test", "FS-000101", EstadoEnvio.FALLIDO, AHORA);
        EnvioCorreo enviado = envio("BIENVENIDA", "ana@fitstore.test", null, EstadoEnvio.ENVIADO, AHORA);

        mvc.perform(post(BASE + "/" + fallido.getId() + "/reenviar").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado", is("PENDIENTE")));
        mvc.perform(post(BASE + "/" + enviado.getId() + "/reenviar").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado", is("PENDIENTE")));

        EnvioCorreo reenviado = envios.findById(fallido.getId()).orElseThrow();
        assertThat(reenviado.getEstado()).isEqualTo(EstadoEnvio.PENDIENTE);
        assertThat(reenviado.getProximoIntentoEn()).isNotNull();
        assertThat(reenviado.getHistorialIntentos()).hasSize(1);
        assertThat(envios.findById(enviado.getId()).orElseThrow().getEstado()).isEqualTo(EstadoEnvio.PENDIENTE);
        // Uno que ya está PENDIENTE no se reenvía otra vez; uno inexistente no existe
        mvc.perform(post(BASE + "/" + fallido.getId() + "/reenviar").with(admin())).andExpect(status().isConflict());
        mvc.perform(post(BASE + "/" + UUID.randomUUID() + "/reenviar").with(admin())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-7-22 un OPERADOR recibe 403 en el registro, el detalle y el reenvío; sin sesión, 401")
    void operadorSinAcceso() throws Exception {
        EnvioCorreo fallido = envio("PEDIDO_CONFIRMADO", "ana@fitstore.test", "FS-000101", EstadoEnvio.FALLIDO, AHORA);

        mvc.perform(get(BASE).with(operador())).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is("SIN_PERMISO")));
        mvc.perform(get(BASE + "/" + fallido.getId()).with(operador())).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/" + fallido.getId() + "/reenviar").with(operador())).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/plantillas").with(operador())).andExpect(status().isForbidden());
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        assertThat(envios.findById(fallido.getId()).orElseThrow().getEstado()).isEqualTo(EstadoEnvio.FALLIDO);
    }

    private static final String TEXTOS = """
            {"asunto": "Gracias por tu compra", "titulo": "Pedido confirmado", "mensaje": "Hola {{nombre}}", "textoBoton": "Ver"}
            """;

    @Test
    @DisplayName("extensión de alcance: las rutas de plantillas conservan su firma y responden 501 EXTENSION_DE_ALCANCE")
    void plantillasSonExtension() throws Exception {
        String plantilla = BASE + "/plantillas/PEDIDO_CONFIRMADO";
        var json = MediaType.APPLICATION_JSON;

        mvc.perform(get(BASE + "/plantillas").with(admin())).andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.codigo", is("EXTENSION_DE_ALCANCE")));
        mvc.perform(get(plantilla).with(admin())).andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.codigo", is("EXTENSION_DE_ALCANCE")));
        mvc.perform(put(plantilla).with(admin()).contentType(json).content(TEXTOS.replace("}\n", ", \"activa\": true}")))
                .andExpect(status().isNotImplemented()).andExpect(jsonPath("$.codigo", is("EXTENSION_DE_ALCANCE")));
        mvc.perform(post(plantilla + "/restaurar").with(admin())).andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.codigo", is("EXTENSION_DE_ALCANCE")));
        mvc.perform(post(plantilla + "/vista-previa").with(admin()).contentType(json).content(TEXTOS))
                .andExpect(status().isNotImplemented()).andExpect(jsonPath("$.codigo", is("EXTENSION_DE_ALCANCE")));
        mvc.perform(post(plantilla + "/prueba").with(admin()).contentType(json).content(TEXTOS))
                .andExpect(status().isNotImplemented()).andExpect(jsonPath("$.codigo", is("EXTENSION_DE_ALCANCE")));
    }
}
