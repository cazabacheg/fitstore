package com.fitstore.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.pruebas.PruebaApi;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** HU-1-06: libreta de direcciones del cliente (CA-1-17 a CA-1-21), por la API y con la base de datos real. */
class DireccionServiceTest extends PruebaApi {

    private static final String RUTA = "/api/v1/clientes/me/direcciones";

    private Cliente ana;
    private String token;

    @BeforeEach
    void clienteConSesion() throws Exception {
        ana = crearCliente("ana@correo.com", "45678912");
        token = tokenCliente("ana@correo.com");
    }

    private static Map<String, Object> direccion(String alias, String ubigeo) {
        Map<String, Object> m = new HashMap<>();
        m.put("alias", alias);
        m.put("ubigeo", ubigeo);
        m.put("direccion", "Jr. Gamarra 123, dpto 4");
        m.put("referencia", "Frente al parque");
        return m;
    }

    private MockHttpServletRequestBuilder conCuerpo(MockHttpServletRequestBuilder peticion, String jwt, Object cuerpo) {
        return peticion.header("Authorization", bearer(jwt)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo));
    }

    private long crear(String alias) throws Exception {
        MvcResult r = mvc.perform(conCuerpo(post(RUTA), token, direccion(alias, "150115"))).andExpect(status().isCreated()).andReturn();
        return cuerpo(r).path("data").path("id").asLong();
    }

    private List<Map<String, Object>> filas(Cliente cliente) {
        return jdbc.queryForList("SELECT id, alias, predeterminada FROM direccion WHERE cliente_id = ?::uuid ORDER BY id",
                cliente.getId().toString());
    }

    private String predeterminadaDe(Cliente cliente) {
        return jdbc.queryForObject("SELECT alias FROM direccion WHERE cliente_id = ?::uuid AND predeterminada",
                String.class, cliente.getId().toString());
    }

    // ---------- CA-1-17 ----------

    @Test
    @DisplayName("CA-1-17 la primera dirección se guarda con su ubigeo de 6 dígitos y queda como predeterminada")
    void laPrimeraEsPredeterminada() throws Exception {
        MvcResult r = mvc.perform(conCuerpo(post(RUTA), token, direccion("Casa", "150115")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.alias").value("Casa"))
                .andExpect(jsonPath("$.data.ubigeo").value("150115"))
                .andExpect(jsonPath("$.data.direccion").value("Jr. Gamarra 123, dpto 4"))
                .andExpect(jsonPath("$.data.referencia").value("Frente al parque"))
                .andExpect(jsonPath("$.data.predeterminada").value(true))
                .andExpect(jsonPath("$.data.telefonoContacto").isEmpty())
                .andReturn();

        long id = cuerpo(r).path("data").path("id").asLong();
        assertThat(r.getResponse().getHeader("Location")).isEqualTo(RUTA + "/" + id);
        assertThat(jdbc.queryForObject("SELECT ubigeo FROM direccion WHERE id = ?", String.class, id).trim()).isEqualTo("150115");
    }

    @Test
    @DisplayName("CA-1-17 las siguientes direcciones no son predeterminadas")
    void lasSiguientesNo() throws Exception {
        crear("Casa");

        mvc.perform(conCuerpo(post(RUTA), token, direccion("Trabajo", "070101")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.predeterminada").value(false));
        assertThat(predeterminadaDe(ana)).isEqualTo("Casa");
    }

    @Test
    @DisplayName("CA-1-17 el listado trae primero la predeterminada y solo las del cliente")
    void listadoConLaPredeterminadaPrimero() throws Exception {
        Cliente otro = crearCliente("otro@correo.com", "11111111");
        String tokenOtro = tokenCliente("otro@correo.com");
        crear("Casa");
        crear("Trabajo");
        mvc.perform(conCuerpo(post(RUTA), tokenOtro, direccion("Ajena", "150101"))).andExpect(status().isCreated());
        long trabajo = ((Number) filas(ana).get(1).get("id")).longValue();
        mvc.perform(patch(RUTA + "/" + trabajo + "/predeterminada").header("Authorization", bearer(token))).andExpect(status().isOk());

        mvc.perform(get(RUTA).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].alias").value("Trabajo"))
                .andExpect(jsonPath("$.data[0].predeterminada").value(true))
                .andExpect(jsonPath("$.data[1].alias").value("Casa"));
        assertThat(filas(otro)).hasSize(1);
    }

    @Test
    @DisplayName("CA-1-17 el ubigeo, la dirección y el celular de contacto se validan")
    void validaciones() throws Exception {
        Map<String, Object> ubigeo = direccion("Casa", "12345");
        Map<String, Object> corta = direccion("Casa", "150115");
        corta.put("direccion", "abc");
        Map<String, Object> celular = direccion("Casa", "150115");
        celular.put("telefonoContacto", "812345678");
        Map<String, Object> sinAlias = direccion("", "150115");

        mvc.perform(conCuerpo(post(RUTA), token, ubigeo)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("ubigeo"));
        mvc.perform(conCuerpo(post(RUTA), token, corta)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("direccion"));
        mvc.perform(conCuerpo(post(RUTA), token, celular)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("telefonoContacto"));
        mvc.perform(conCuerpo(post(RUTA), token, sinAlias)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("alias"));
        assertThat(filas(ana)).isEmpty();
    }

    // ---------- CA-1-18 ----------

    @Test
    @DisplayName("CA-1-18 al marcar otra como predeterminada, la anterior deja de serlo (siempre una sola)")
    void cambiaLaPredeterminada() throws Exception {
        crear("Casa");
        long trabajo = crear("Trabajo");

        mvc.perform(patch(RUTA + "/" + trabajo + "/predeterminada").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        assertThat(predeterminadaDe(ana)).isEqualTo("Trabajo");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM direccion WHERE cliente_id = ?::uuid AND predeterminada",
                Integer.class, ana.getId().toString())).isEqualTo(1);
    }

    // ---------- CA-1-19 ----------

    @Test
    @DisplayName("CA-1-19 al editar una dirección se actualizan sus datos y se conserva su condición de predeterminada")
    void editaUnaDireccion() throws Exception {
        long id = crear("Casa");
        Map<String, Object> cambios = direccion("Oficina", "070101");
        cambios.put("telefonoContacto", "912345678");

        mvc.perform(conCuerpo(put(RUTA + "/" + id), token, cambios))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alias").value("Oficina"))
                .andExpect(jsonPath("$.data.ubigeo").value("070101"))
                .andExpect(jsonPath("$.data.telefonoContacto").value("912345678"))
                .andExpect(jsonPath("$.data.predeterminada").value(true));
        assertThat(jdbc.queryForObject("SELECT alias FROM direccion WHERE id = ?", String.class, id)).isEqualTo("Oficina");
    }

    @Test
    @DisplayName("CA-1-19 al eliminar se borra físicamente; si era la predeterminada pasa a serlo la creada más recientemente")
    void eliminaYReasignaLaPredeterminada() throws Exception {
        crear("Casa");
        long trabajo = crear("Trabajo");
        crear("Gimnasio");
        mvc.perform(patch(RUTA + "/" + trabajo + "/predeterminada").header("Authorization", bearer(token))).andExpect(status().isOk());

        mvc.perform(delete(RUTA + "/" + trabajo).header("Authorization", bearer(token))).andExpect(status().isOk());

        assertThat(filas(ana)).extracting(f -> f.get("alias")).containsExactly("Casa", "Gimnasio");
        assertThat(predeterminadaDe(ana)).isEqualTo("Gimnasio");
    }

    @Test
    @DisplayName("CA-1-19 eliminar una dirección que no es la predeterminada no cambia cuál lo es; la última se puede borrar")
    void eliminaUnaComun() throws Exception {
        crear("Casa");
        long trabajo = crear("Trabajo");

        mvc.perform(delete(RUTA + "/" + trabajo).header("Authorization", bearer(token))).andExpect(status().isOk());
        assertThat(predeterminadaDe(ana)).isEqualTo("Casa");

        long casa = ((Number) filas(ana).get(0).get("id")).longValue();
        mvc.perform(delete(RUTA + "/" + casa).header("Authorization", bearer(token))).andExpect(status().isOk());
        assertThat(filas(ana)).isEmpty();
    }

    // ---------- CA-1-20 ----------

    @Test
    @DisplayName("CA-1-20 con 10 direcciones, crear otra responde 409 LIMITE_DIRECCIONES")
    void limiteDeDiez() throws Exception {
        for (int i = 1; i <= 10; i++) {
            crear("Dirección " + i);
        }

        mvc.perform(conCuerpo(post(RUTA), token, direccion("Once", "150115")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("LIMITE_DIRECCIONES"));
        assertThat(filas(ana)).hasSize(10);

        // liberar un lugar permite crear de nuevo
        long primera = ((Number) filas(ana).get(0).get("id")).longValue();
        mvc.perform(delete(RUTA + "/" + primera).header("Authorization", bearer(token))).andExpect(status().isOk());
        mvc.perform(conCuerpo(post(RUTA), token, direccion("Once", "150115"))).andExpect(status().isCreated());
    }

    // ---------- CA-1-21 ----------

    @Test
    @DisplayName("CA-1-21 una dirección de otro cliente responde 404 al ver, editar, borrar o marcar, y no se toca")
    void direccionAjena() throws Exception {
        Cliente otro = crearCliente("otro@correo.com", "11111111");
        String tokenOtro = tokenCliente("otro@correo.com");
        MvcResult creada = mvc.perform(conCuerpo(post(RUTA), tokenOtro, direccion("Ajena", "150101"))).andReturn();
        long ajena = cuerpo(creada).path("data").path("id").asLong();
        String ruta = RUTA + "/" + ajena;

        mvc.perform(get(ruta).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
        mvc.perform(conCuerpo(put(ruta), token, direccion("Robada", "150115"))).andExpect(status().isNotFound());
        mvc.perform(patch(ruta + "/predeterminada").header("Authorization", bearer(token))).andExpect(status().isNotFound());
        mvc.perform(delete(ruta).header("Authorization", bearer(token))).andExpect(status().isNotFound());

        assertThat(filas(otro)).hasSize(1);
        assertThat(filas(otro).get(0).get("alias")).isEqualTo("Ajena");
        mvc.perform(get(ruta).header("Authorization", bearer(tokenOtro))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alias").value("Ajena"));
    }

    @Test
    @DisplayName("CA-1-21 una dirección que no existe responde 404")
    void direccionInexistente() throws Exception {
        mvc.perform(get(RUTA + "/999999").header("Authorization", bearer(token))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-1-21 las direcciones solo las atiende una sesión de cliente: sin token 401, con staff 403")
    void soloElCliente() throws Exception {
        crearStaff("admin@fitstore.pe", RolStaff.ADMIN);
        String staff = tokenStaff("admin@fitstore.pe");

        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mvc.perform(get(RUTA).header("Authorization", bearer(staff))).andExpect(status().isForbidden());
        mvc.perform(conCuerpo(post(RUTA), staff, direccion("Casa", "150115"))).andExpect(status().isForbidden());
        mvc.perform(post(RUTA).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
