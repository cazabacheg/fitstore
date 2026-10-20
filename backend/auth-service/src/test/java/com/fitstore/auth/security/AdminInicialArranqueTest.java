package com.fitstore.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.pruebas.PostgresAislada;
import com.fitstore.auth.pruebas.RedisPrueba;
import com.fitstore.auth.repository.StaffRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/** El servicio completo arranca con las variables del admin inicial sobre una base vacía (CA-1-38). */
@SpringBootTest(properties = {"FITSTORE_ADMIN_EMAIL=admin.inicial@fitstore.pe", "FITSTORE_ADMIN_PASSWORD=ClaveInicial2026"})
@AutoConfigureMockMvc
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = {PostgresAislada.class, RedisPrueba.class})
class AdminInicialArranqueTest {

    @Autowired StaffRepository personal;
    @Autowired MockMvc mvc;

    @Test
    @DisplayName("CA-1-38 al arrancar sin ADMIN crea el de las variables de entorno y ese ADMIN puede ingresar")
    void creaYIngresa() throws Exception {
        assertThat(personal.findAll()).hasSize(1);
        assertThat(personal.findByEmailIgnoreCase("admin.inicial@fitstore.pe")).get()
                .satisfies(admin -> assertThat(admin.getRol()).isEqualTo(RolStaff.ADMIN));

        mvc.perform(post("/api/v1/auth/staff/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin.inicial@fitstore.pe\",\"password\":\"ClaveInicial2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cuenta.rol").value("ADMIN"));
    }
}
