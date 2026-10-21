package com.fitstore.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.repository.StaffRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Admin inicial (D6, CA-1-38): se crea solo si no hay ningún ADMIN y las dos variables están definidas. */
class AdminInicialTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");

    private final StaffRepository personal = mock(StaffRepository.class);
    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder(4);
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);

    private AdminInicial con(String email, String password) {
        return new AdminInicial(personal, codificador, reloj, email, password);
    }

    @Test
    @DisplayName("CA-1-38 sin ningún ADMIN y con las dos variables, crea ese ADMIN activo con su contraseña con BCrypt")
    void creaElAdmin() {
        when(personal.existsByRol(RolStaff.ADMIN)).thenReturn(false);

        con("  Admin@FitStore.pe ", "ClaveInicial2026").run(null);

        ArgumentCaptor<Staff> guardado = ArgumentCaptor.forClass(Staff.class);
        verify(personal).save(guardado.capture());
        Staff admin = guardado.getValue();
        assertThat(admin.getEmail()).isEqualTo("admin@fitstore.pe");
        assertThat(admin.getRol()).isEqualTo(RolStaff.ADMIN);
        assertThat(admin.isActivo()).isTrue();
        assertThat(admin.getPasswordHash()).isNotEqualTo("ClaveInicial2026");
        assertThat(codificador.matches("ClaveInicial2026", admin.getPasswordHash())).isTrue();
        assertThat(admin.getNombres()).isNotBlank();
        assertThat(admin.getApellidos()).isNotBlank();
        assertThat(admin.getCreadoPor()).isNull();
        assertThat(admin.getCredencialesActualizadasEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("CA-1-38 si ya existe un ADMIN no hace nada")
    void yaExisteUnAdmin() {
        when(personal.existsByRol(RolStaff.ADMIN)).thenReturn(true);

        con("admin@fitstore.pe", "ClaveInicial2026").run(null);

        verify(personal, never()).save(any());
    }

    @Test
    @DisplayName("CA-1-38 sin el correo o sin la contraseña no crea nada")
    void sinVariables() {
        when(personal.existsByRol(RolStaff.ADMIN)).thenReturn(false);

        con("", "ClaveInicial2026").run(null);
        con("admin@fitstore.pe", "").run(null);
        con(null, null).run(null);

        verify(personal, never()).save(any());
    }

    @Test
    @DisplayName("CA-1-38 si el correo ya es de un miembro del staff que no es ADMIN, no crea una cuenta repetida")
    void correoYaUsado() {
        when(personal.existsByRol(RolStaff.ADMIN)).thenReturn(false);
        when(personal.existsByEmailIgnoreCase("op@fitstore.pe")).thenReturn(true);

        con("op@fitstore.pe", "ClaveInicial2026").run(null);

        verify(personal, never()).save(any());
    }
}
