package com.fitstore.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.Direccion;
import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.pruebas.Datos;
import com.fitstore.auth.pruebas.PruebaApi;
import com.fitstore.auth.repository.DireccionRepository;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** HU-1-09: consulta de clientes desde el backoffice y bloqueo por un ADMIN (CA-1-32 a CA-1-34). */
class ClienteAdminControllerTest extends PruebaApi {

    private static final String RUTA = "/api/v1/clientes";

    @Autowired DireccionRepository direcciones;

    private Cliente ana;
    private Cliente beto;
    private String admin;
    private String operador;

    @BeforeEach
    void datos() throws Exception {
        ana = crearCliente("ana.quispe@correo.com", "45678912");
        ana.setNombres("Ana");
        ana.setApellidos("Quispe Rojas");
        clientes.saveAndFlush(ana);
        beto = crearCliente("beto@correo.com", "11112222");
        beto.setNombres("Beto");
        beto.setApellidos("Mamani Lazo");
        beto.setActivo(false);
        clientes.saveAndFlush(beto);
        crearStaff("admin@fitstore.pe", RolStaff.ADMIN);
        crearStaff("op@fitstore.pe", RolStaff.OPERADOR);
        admin = tokenStaff("admin@fitstore.pe");
        operador = tokenStaff("op@fitstore.pe");
    }

    private Direccion direccion(Cliente cliente, String alias, boolean predeterminada) {
        return direcciones.saveAndFlush(Datos.direccion(cliente, alias, predeterminada));
    }

    // ---------- CA-1-32 ----------

    @Test
    @DisplayName("CA-1-32 lista paginada de clientes; un OPERADOR y un ADMIN pueden consultarla")
    void listaPaginada() throws Exception {
        for (String token : new String[] {operador, admin}) {
            mvc.perform(get(RUTA).header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElementos").value(2))
                    .andExpect(jsonPath("$.data.pagina").value(0))
                    .andExpect(jsonPath("$.data.tamano").value(20))
                    .andExpect(jsonPath("$.data.totalPaginas").value(1))
                    .andExpect(jsonPath("$.data.contenido.length()").value(2))
                    .andExpect(jsonPath("$.data.contenido[0].passwordHash").doesNotExist());
        }
        mvc.perform(get(RUTA).param("size", "1").param("page", "1").param("sort", "email,asc").header("Authorization", bearer(operador)))
                .andExpect(jsonPath("$.data.contenido.length()").value(1))
                .andExpect(jsonPath("$.data.contenido[0].email").value("beto@correo.com"))
                .andExpect(jsonPath("$.data.totalPaginas").value(2));
    }

    @Test
    @DisplayName("CA-1-32 busca por nombre, por correo o por DNI, sin importar mayúsculas")
    void buscaPorNombreCorreoODni() throws Exception {
        mvc.perform(get(RUTA).param("q", "QUISPE").header("Authorization", bearer(operador)))
                .andExpect(jsonPath("$.data.totalElementos").value(1))
                .andExpect(jsonPath("$.data.contenido[0].email").value("ana.quispe@correo.com"))
                .andExpect(jsonPath("$.data.contenido[0].dni").value("45678912"))
                .andExpect(jsonPath("$.data.contenido[0].activo").value(true));
        mvc.perform(get(RUTA).param("q", "Beto@Correo").header("Authorization", bearer(operador)))
                .andExpect(jsonPath("$.data.totalElementos").value(1))
                .andExpect(jsonPath("$.data.contenido[0].activo").value(false));
        mvc.perform(get(RUTA).param("q", "1111").header("Authorization", bearer(operador)))
                .andExpect(jsonPath("$.data.totalElementos").value(1))
                .andExpect(jsonPath("$.data.contenido[0].email").value("beto@correo.com"));
        mvc.perform(get(RUTA).param("q", "no-existe").header("Authorization", bearer(operador)))
                .andExpect(jsonPath("$.data.totalElementos").value(0));
    }

    @Test
    @DisplayName("CA-1-32 filtra por activo o bloqueado y un comodín del usuario se toma literalmente")
    void filtraPorActivo() throws Exception {
        mvc.perform(get(RUTA).param("activo", "false").header("Authorization", bearer(operador)))
                .andExpect(jsonPath("$.data.totalElementos").value(1))
                .andExpect(jsonPath("$.data.contenido[0].email").value("beto@correo.com"));
        mvc.perform(get(RUTA).param("activo", "true").header("Authorization", bearer(operador)))
                .andExpect(jsonPath("$.data.totalElementos").value(1))
                .andExpect(jsonPath("$.data.contenido[0].email").value("ana.quispe@correo.com"));
        mvc.perform(get(RUTA).param("q", "%").header("Authorization", bearer(operador)))
                .andExpect(jsonPath("$.data.totalElementos").value(0));
    }

    // ---------- CA-1-33 ----------

    @Test
    @DisplayName("CA-1-33 el detalle trae los datos del cliente y sus direcciones, la predeterminada primero")
    void detalleConDirecciones() throws Exception {
        direccion(ana, "Casa", false);
        direccion(ana, "Trabajo", true);

        mvc.perform(get(RUTA + "/" + ana.getId()).header("Authorization", bearer(operador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ana.getId().toString()))
                .andExpect(jsonPath("$.data.email").value("ana.quispe@correo.com"))
                .andExpect(jsonPath("$.data.dni").value("45678912"))
                .andExpect(jsonPath("$.data.activo").value(true))
                .andExpect(jsonPath("$.data.direcciones.length()").value(2))
                .andExpect(jsonPath("$.data.direcciones[0].alias").value("Trabajo"))
                .andExpect(jsonPath("$.data.direcciones[0].predeterminada").value(true))
                .andExpect(jsonPath("$.data.direcciones[1].alias").value("Casa"));
    }

    @Test
    @DisplayName("CA-1-33 un cliente que no existe responde 404")
    void detalleInexistente() throws Exception {
        mvc.perform(get(RUTA + "/" + UUID.randomUUID()).header("Authorization", bearer(operador)))
                .andExpect(status().isNotFound());
    }

    // ---------- CA-1-34 ----------

    @Test
    @DisplayName("CA-1-34 un ADMIN bloquea a un cliente: no ingresa ni refresca; al desbloquear vuelve a ingresar")
    void adminBloqueaYDesbloquea() throws Exception {
        Cookie sesion = mvc.perform(postJson("/api/v1/auth/login", Map.of("email", "ana.quispe@correo.com", "password", PASSWORD)))
                .andReturn().getResponse().getCookie("fs_refresh");

        mvc.perform(patch(RUTA + "/" + ana.getId() + "/desactivar").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activo").value(false));

        mvc.perform(postJson("/api/v1/auth/login", Map.of("email", "ana.quispe@correo.com", "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(sesion)).andExpect(status().isUnauthorized());

        mvc.perform(patch(RUTA + "/" + ana.getId() + "/activar").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activo").value(true));
        mvc.perform(postJson("/api/v1/auth/login", Map.of("email", "ana.quispe@correo.com", "password", PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CA-1-34 un OPERADOR recibe 403 al bloquear o desbloquear y el cliente no cambia")
    void operadorNoBloquea() throws Exception {
        mvc.perform(patch(RUTA + "/" + ana.getId() + "/desactivar").header("Authorization", bearer(operador)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        mvc.perform(patch(RUTA + "/" + beto.getId() + "/activar").header("Authorization", bearer(operador)))
                .andExpect(status().isForbidden());

        assertThat(clientes.findById(ana.getId()).orElseThrow().isActivo()).isTrue();
        assertThat(clientes.findById(beto.getId()).orElseThrow().isActivo()).isFalse();
    }

    @Test
    @DisplayName("CA-1-34 bloquear o desbloquear un cliente inexistente responde 404")
    void bloquearInexistente() throws Exception {
        mvc.perform(patch(RUTA + "/" + UUID.randomUUID() + "/desactivar").header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
        mvc.perform(patch(RUTA + "/" + UUID.randomUUID() + "/activar").header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-1-32 un cliente no consulta a otros clientes (403) y sin token responde 401")
    void soloElStaffConsulta() throws Exception {
        String cliente = tokenCliente("ana.quispe@correo.com");

        mvc.perform(get(RUTA).header("Authorization", bearer(cliente))).andExpect(status().isForbidden());
        mvc.perform(get(RUTA + "/" + beto.getId()).header("Authorization", bearer(cliente))).andExpect(status().isForbidden());
        mvc.perform(patch(RUTA + "/" + beto.getId() + "/desactivar").header("Authorization", bearer(cliente)))
                .andExpect(status().isForbidden());
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }
}
