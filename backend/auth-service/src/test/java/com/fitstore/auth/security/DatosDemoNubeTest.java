package com.fitstore.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.pruebas.Datos;
import com.fitstore.auth.repository.ClienteRepository;
import com.fitstore.auth.repository.StaffRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Cuentas de demostración en la nube (A7, CA-1-39): nunca sirve la contraseña de desarrollo. */
class DatosDemoNubeTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");
    private static final String HASH_DE_DESARROLLO = "$2a$10$w7ld0Xz.lYUKeVGAx122Ee5NMUnj/qGMCGE/w6UjvJqTdizhKF3AG";

    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final StaffRepository personal = mock(StaffRepository.class);
    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder(4);
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    private Cliente demo1;
    private Cliente demo2;
    private Staff operador;

    @BeforeEach
    void cuentas() {
        demo1 = demoCliente("lucia@fitstore.demo", "70123401", 0);
        demo2 = demoCliente("carlos@fitstore.demo", "70123402", 1);
        operador = Datos.staff("diana@fitstore.demo", RolStaff.OPERADOR);
        operador.setId(CuentasDemo.STAFF.get(0));
        operador.setPasswordHash(HASH_DE_DESARROLLO);
        // solo dos de los cuatro clientes existen en esta base
        when(clientes.findAllById(CuentasDemo.CLIENTES)).thenReturn(List.of(demo1, demo2));
        when(personal.findAllById(CuentasDemo.STAFF)).thenReturn(List.of(operador));
    }

    private Cliente demoCliente(String email, String dni, int indice) {
        Cliente c = Datos.cliente(email, dni);
        c.setId(CuentasDemo.CLIENTES.get(indice));
        c.setPasswordHash(HASH_DE_DESARROLLO);
        return c;
    }

    @Test
    @DisplayName("CA-1-39 con DEMO_PASSWORD reemplaza la contraseña de todas las cuentas de demostración por esa")
    void reemplazaLaContrasena() {
        new DatosDemoNube(clientes, personal, codificador, reloj, "NubeSecreta2026").run(null);

        for (var cuenta : List.of(demo1, demo2, operador)) {
            assertThat(codificador.matches("NubeSecreta2026", cuenta.getPasswordHash())).isTrue();
            assertThat(cuenta.getPasswordHash()).isNotEqualTo(HASH_DE_DESARROLLO);
            assertThat(cuenta.isActivo()).isTrue();
            assertThat(cuenta.getCredencialesActualizadasEn()).isEqualTo(AHORA);
        }
        verify(clientes).saveAll(List.of(demo1, demo2));
        verify(personal).saveAll(List.of(operador));
    }

    @Test
    @DisplayName("CA-1-39 sin DEMO_PASSWORD desactiva las cuentas de demostración y no deja la contraseña de desarrollo usable")
    void sinContrasenaDesactiva() {
        for (String vacia : new String[] {"", "   ", null}) {
            demo1.setActivo(true);
            demo2.setActivo(true);
            operador.setActivo(true);

            new DatosDemoNube(clientes, personal, codificador, reloj, vacia).run(null);

            for (var cuenta : List.of(demo1, demo2, operador)) {
                assertThat(cuenta.isActivo()).isFalse();
                assertThat(cuenta.getCredencialesActualizadasEn()).isEqualTo(AHORA);
            }
        }
        verify(clientes, org.mockito.Mockito.times(3)).saveAll(List.of(demo1, demo2));
    }

    @Test
    @DisplayName("CA-1-39 solo corre con el perfil demo")
    void soloConElPerfilDemo() {
        Profile perfil = DatosDemoNube.class.getAnnotation(Profile.class);

        assertThat(perfil).isNotNull();
        assertThat(perfil.value()).containsExactly("demo");
    }

    @Test
    @DisplayName("CA-1-39 los ids de las cuentas de demostración son los de docs/demo/ids-demo.md")
    void idsFijos() {
        assertThat(CuentasDemo.CLIENTES).hasSize(4).first().hasToString("35a11d4f-054a-44a5-8ac4-cf0ed4020f8c");
        assertThat(CuentasDemo.STAFF).containsExactly(java.util.UUID.fromString("5e738102-a490-469f-b0f6-5f1fc2dbf70c"));
    }
}
