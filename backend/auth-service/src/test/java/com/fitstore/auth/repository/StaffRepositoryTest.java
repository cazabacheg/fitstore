package com.fitstore.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.pruebas.Datos;
import com.fitstore.auth.pruebas.PruebaRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

@PruebaRepositorio
class StaffRepositoryTest {

    @Autowired StaffRepository repositorio;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("CA-0a-06 StaffRepository inserta un staff sin contraseña (invitación pendiente)")
    void insertaSinPassword() {
        Staff guardado = repositorio.saveAndFlush(Datos.staff("op@fitstore.pe", RolStaff.OPERADOR));

        Staff leido = repositorio.findById(guardado.getId()).orElseThrow();
        assertThat(leido.getPasswordHash()).isNull();
        assertThat(leido.getRol()).isEqualTo(RolStaff.OPERADOR);
        assertThat(leido.getCreadoPor()).isNull();
        assertThat(leido.getCreadoEn()).isNotNull();
    }

    @Test
    @DisplayName("CA-0a-06 StaffRepository actualiza nombre y rol")
    void actualiza() {
        Staff s = repositorio.saveAndFlush(Datos.staff("op2@fitstore.pe", RolStaff.OPERADOR));
        s.setNombres("Mariela");
        s.setRol(RolStaff.ADMIN);
        repositorio.saveAndFlush(s);

        Staff leido = repositorio.findById(s.getId()).orElseThrow();
        assertThat(leido.getNombres()).isEqualTo("Mariela");
        assertThat(leido.getRol()).isEqualTo(RolStaff.ADMIN);
    }

    @Test
    @DisplayName("CA-0a-06 StaffRepository elimina de forma lógica")
    void eliminaLogicamente() {
        Staff s = repositorio.saveAndFlush(Datos.staff("op3@fitstore.pe", RolStaff.OPERADOR));
        s.setActivo(false);
        repositorio.saveAndFlush(s);

        assertThat(repositorio.findById(s.getId())).get().extracting(Staff::isActivo).isEqualTo(false);
    }

    @Test
    @DisplayName("CA-0a-06 StaffRepository lista paginado por rol")
    void listaPorRol() {
        repositorio.save(Datos.staff("a1@fitstore.pe", RolStaff.ADMIN));
        repositorio.save(Datos.staff("o1@fitstore.pe", RolStaff.OPERADOR));
        repositorio.save(Datos.staff("o2@fitstore.pe", RolStaff.OPERADOR));
        repositorio.save(Datos.staff("o3@fitstore.pe", RolStaff.OPERADOR));
        repositorio.flush();

        var pagina = repositorio.findByRol(RolStaff.OPERADOR, PageRequest.of(0, 2));
        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getContent()).hasSize(2).allMatch(s -> s.getRol() == RolStaff.OPERADOR);
    }

    @Test
    @DisplayName("CA-0a-06 StaffRepository cuenta los ADMIN activos (invariante del último admin)")
    void cuentaAdminsActivos() {
        repositorio.save(Datos.staff("adm1@fitstore.pe", RolStaff.ADMIN));
        Staff inactivo = Datos.staff("adm2@fitstore.pe", RolStaff.ADMIN);
        inactivo.setActivo(false);
        repositorio.save(inactivo);
        repositorio.save(Datos.staff("op@fitstore.pe", RolStaff.OPERADOR));
        repositorio.flush();

        assertThat(repositorio.countByRolAndActivoTrue(RolStaff.ADMIN)).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-0a-06 StaffRepository registra quién dio de alta (creado_por)")
    void registraCreadoPor() {
        Staff admin = repositorio.saveAndFlush(Datos.staff("raiz@fitstore.pe", RolStaff.ADMIN));
        Staff nuevo = Datos.staff("nuevo@fitstore.pe", RolStaff.OPERADOR);
        nuevo.setCreadoPor(admin.getId());
        repositorio.saveAndFlush(nuevo);

        assertThat(repositorio.findById(nuevo.getId())).get().extracting(Staff::getCreadoPor).isEqualTo(admin.getId());
    }

    @Test
    @DisplayName("CA-0a-06 StaffRepository rechaza un creado_por que no existe (FK)")
    void rechazaCreadoPorInexistente() {
        Staff s = Datos.staff("huerfano@fitstore.pe", RolStaff.OPERADOR);
        s.setCreadoPor(java.util.UUID.randomUUID());

        assertThatThrownBy(() -> repositorio.saveAndFlush(s)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 StaffRepository rechaza un correo duplicado sin importar mayúsculas y consulta por correo")
    void correoDuplicado() {
        repositorio.saveAndFlush(Datos.staff("mixto@fitstore.pe", RolStaff.OPERADOR));

        assertThat(repositorio.existsByEmailIgnoreCase("MIXTO@fitstore.PE")).isTrue();
        assertThat(repositorio.findByEmailIgnoreCase("Mixto@fitstore.pe")).isPresent();
        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.staff("MIXTO@FITSTORE.PE", RolStaff.ADMIN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 StaffRepository rechaza un rol fuera de OPERADOR y ADMIN (CHECK)")
    void rechazaRolInvalido() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO staff (id, email, nombres, apellidos, rol, activo, credenciales_actualizadas_en,
                                   creado_en, actualizado_en)
                VALUES (gen_random_uuid(), 'x@fitstore.pe', 'X', 'Y', 'SUPERUSUARIO', true, now(), now(), now())
                """)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
