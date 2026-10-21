package com.fitstore.delivery.api;

import static com.fitstore.delivery.pruebas.Sesiones.admin;
import static com.fitstore.delivery.pruebas.Sesiones.operador;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.delivery.model.Repartidor;
import com.fitstore.delivery.model.TipoVehiculo;
import com.fitstore.delivery.pruebas.Pedidos;
import com.fitstore.delivery.pruebas.PruebaApi;
import com.fitstore.delivery.repository.RepartidorRepository;
import com.fitstore.delivery.service.EntregaService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** API de entregas del backoffice: permisos, avances, conflictos y listado con filtros (CA-6-20 a CA-6-32). */
class AdminEntregaControllerTest extends PruebaApi {

    @Autowired EntregaService servicio;
    @Autowired RepartidorRepository repartidores;

    private Repartidor repartidor() {
        return repartidores.save(Repartidor.builder().nombres("Juan Pérez").telefono("987654321")
                .vehiculo(TipoVehiculo.MOTO).placa("ABC-123").build());
    }

    private UUID nueva(String codigo) {
        return servicio.crearDesde(Pedidos.local(codigo)).id();
    }

    private ResultActions patchear(UUID id, String accion, String cuerpo) throws Exception {
        return mvc.perform(patch("/api/v1/admin/entregas/" + id + "/" + accion).with(operador())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    @Test
    @DisplayName("CA-6-20 asignar por la API devuelve la entrega ASIGNADA con sus acciones permitidas")
    void asignaPorLaApi() throws Exception {
        Repartidor juan = repartidor();
        UUID id = nueva("FS-000301");

        patchear(id, "asignar", "{\"repartidorId\":" + juan.getId() + ",\"fechaProgramada\":\"2026-10-19\",\"version\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("ASIGNADA"))
                .andExpect(jsonPath("$.data.repartidor.nombres").value("Juan Pérez"))
                .andExpect(jsonPath("$.data.accionesPermitidas", contains("REPROGRAMAR", "EN_RUTA")))
                .andExpect(jsonPath("$.data.version").value(1));
    }

    @Test
    @DisplayName("CA-6-21 el detalle trae el seguimiento con el nombre de quien hizo cada paso")
    void detalleConSeguimiento() throws Exception {
        Repartidor juan = repartidor();
        UUID id = nueva("FS-000302");
        patchear(id, "asignar", "{\"repartidorId\":" + juan.getId() + ",\"fechaProgramada\":\"2026-10-20\",\"version\":0}");
        patchear(id, "en-ruta", "{\"version\":1}").andExpect(status().isOk());

        mvc.perform(get("/api/v1/admin/entregas/" + id).with(operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("EN_RUTA"))
                .andExpect(jsonPath("$.data.eventos", hasSize(3)))
                .andExpect(jsonPath("$.data.eventos[2].estado").value("EN_RUTA"))
                .andExpect(jsonPath("$.data.eventos[2].actorNombre").value("Olga Operadora"))
                .andExpect(jsonPath("$.data.destinatario.dni").value("45678912"))
                .andExpect(jsonPath("$.data.accionesPermitidas", contains("ENTREGADA", "FALLIDA")));
    }

    @Test
    @DisplayName("CA-6-22 No se pudo entregar sin observación da 400 y con ella deja la entrega FALLIDA")
    void fallidaPorLaApi() throws Exception {
        Repartidor juan = repartidor();
        UUID id = nueva("FS-000303");
        patchear(id, "asignar", "{\"repartidorId\":" + juan.getId() + ",\"fechaProgramada\":\"2026-10-19\",\"version\":0}");
        patchear(id, "en-ruta", "{\"version\":1}");

        patchear(id, "fallida", "{\"observacion\":\"\",\"version\":2}").andExpect(status().isBadRequest());
        patchear(id, "fallida", "{\"observacion\":\"Dirección incorrecta\",\"version\":2}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("FALLIDA"))
                .andExpect(jsonPath("$.data.siguienteAccion").value("REPROGRAMAR"));
    }

    @Test
    @DisplayName("CA-6-23 una transición no permitida da 409 con el estado actual")
    void conflictoDeEstado() throws Exception {
        UUID id = nueva("FS-000304");

        patchear(id, "en-ruta", "{\"version\":0}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_NO_PERMITIDA"))
                .andExpect(jsonPath("$.detalles.estadoActual").value("PENDIENTE_ASIGNACION"));
        patchear(id, "disponible-agencia", "{\"version\":0}").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CA-6-24 un avance con la versión vieja da 409 'La entrega cambió; recarga la página'")
    void conflictoDeVersion() throws Exception {
        Repartidor juan = repartidor();
        UUID id = nueva("FS-000305");
        patchear(id, "asignar", "{\"repartidorId\":" + juan.getId() + ",\"fechaProgramada\":\"2026-10-19\",\"version\":0}");

        patchear(id, "en-ruta", "{\"version\":0}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La entrega cambió; recarga la página"));
    }

    @Test
    @DisplayName("CA-6-26 registrar el envío por agencia con el código devuelve la URL de rastreo")
    void envioPorAgencia() throws Exception {
        Long shalom = jdbc.queryForObject("SELECT id FROM agencia WHERE nombre = 'Shalom'", Long.class);
        UUID id = servicio.crearDesde(Pedidos.agencia("FS-000306", shalom, "Shalom")).id();

        patchear(id, "enviada-agencia", "{\"codigoSeguimiento\":\"SH-5566\",\"claveRecojo\":\"1234\",\"version\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("ENVIADO_AGENCIA"))
                .andExpect(jsonPath("$.data.urlSeguimiento").value("https://rastreo.ejemplo.pe/shalom?codigo=SH-5566"));
        patchear(id, "enviada-agencia", "{\"codigoSeguimiento\":\"\",\"version\":1}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-6-31 el listado trae las pendientes primero y los contadores por estado de la pestaña")
    void listaConContadores() throws Exception {
        Repartidor juan = repartidor();
        UUID asignada = nueva("FS-000311");
        nueva("FS-000312");
        servicio.crearDesde(Pedidos.agencia("FS-000313", null, null));
        patchear(asignada, "asignar", "{\"repartidorId\":" + juan.getId() + ",\"fechaProgramada\":\"2026-10-19\",\"version\":0}");

        mvc.perform(get("/api/v1/admin/entregas").with(operador()).param("modalidad", "DELIVERY_LOCAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido", hasSize(2)))
                .andExpect(jsonPath("$.data.contenido[0].codigoPedido").value("FS-000312"))
                .andExpect(jsonPath("$.data.contenido[0].siguienteAccion").value("ASIGNAR"))
                .andExpect(jsonPath("$.data.contenido[1].codigoPedido").value("FS-000311"))
                .andExpect(jsonPath("$.data.totalElementos").value(2))
                .andExpect(jsonPath("$.data.contadores.PENDIENTE_ASIGNACION").value(1))
                .andExpect(jsonPath("$.data.contadores.ASIGNADA").value(1))
                .andExpect(jsonPath("$.data.contadores.PENDIENTE_ENVIO").value(0));
    }

    @Test
    @DisplayName("CA-6-32 filtra por estado, fecha, repartidor y código de pedido a la vez")
    void filtra() throws Exception {
        Repartidor juan = repartidor();
        UUID uno = nueva("FS-000321");
        UUID dos = nueva("FS-000322");
        nueva("FS-000323");
        patchear(uno, "asignar", "{\"repartidorId\":" + juan.getId() + ",\"fechaProgramada\":\"2026-10-19\",\"version\":0}");
        patchear(dos, "asignar", "{\"repartidorId\":" + juan.getId() + ",\"fechaProgramada\":\"2026-10-21\",\"version\":0}");

        mvc.perform(get("/api/v1/admin/entregas").with(admin())
                        .param("estado", "ASIGNADA").param("fecha", "2026-10-21")
                        .param("repartidorId", String.valueOf(juan.getId())).param("codigo", "322"))
                .andExpect(jsonPath("$.data.contenido", hasSize(1)))
                .andExpect(jsonPath("$.data.contenido[0].codigoPedido").value("FS-000322"))
                // Los contadores no aplican el filtro de estado, pero sí los demás
                .andExpect(jsonPath("$.data.contadores.ASIGNADA").value(1))
                .andExpect(jsonPath("$.data.contadores.PENDIENTE_ASIGNACION").value(0));
        mvc.perform(get("/api/v1/admin/entregas").with(admin()).param("estado", "PENDIENTE_ASIGNACION"))
                .andExpect(jsonPath("$.data.contenido", hasSize(1)))
                .andExpect(jsonPath("$.data.contenido[0].codigoPedido").value("FS-000323"));
    }

    @Test
    @DisplayName("CA-6-31 sin sesión da 401 y el detalle de una entrega inexistente da 404")
    void seguridadYNoEncontrada() throws Exception {
        mvc.perform(get("/api/v1/admin/entregas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/entregas/" + UUID.randomUUID()).with(operador())).andExpect(status().isNotFound());
    }
}
