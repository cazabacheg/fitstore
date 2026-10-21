package com.fitstore.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitstore.auth.exception.CredencialesInvalidasException;
import com.fitstore.auth.exception.CuentaBloqueadaException;
import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.Cuenta;
import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.model.TipoCuenta;
import com.fitstore.auth.pruebas.Datos;
import com.fitstore.auth.security.CuentaLookup;
import com.fitstore.auth.security.ParTokens;
import com.fitstore.auth.security.RegistroIntentosLogin;
import com.fitstore.auth.security.SesionEmitida;
import com.fitstore.auth.security.TokenService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Lógica del login con sus dependencias simuladas (HU-1-02 y HU-1-07). El bloqueo real con Redis: RegistroIntentosLoginTest. */
class AutenticacionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");

    private final CuentaLookup cuentas = mock(CuentaLookup.class);
    private final RegistroIntentosLogin intentos = mock(RegistroIntentosLogin.class);
    private final PasswordEncoder codificador = spy(new BCryptPasswordEncoder(4));
    private final TokenService tokens = mock(TokenService.class);
    private AutenticacionServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new AutenticacionServiceImpl(cuentas, intentos, codificador, tokens, Clock.fixed(AHORA, ZoneOffset.UTC));
        when(tokens.emitir(any(Cuenta.class))).thenReturn(new ParTokens("acceso", AHORA, "refresh", AHORA));
    }

    private Cliente clienteConPassword(String password) {
        Cliente c = Datos.cliente("ana@correo.com", "45678912");
        c.setId(UUID.randomUUID());
        c.setPasswordHash(codificador.encode(password));
        return c;
    }

    @Test
    @DisplayName("CA-1-04 con credenciales correctas emite los tokens, borra los fallos y registra el último ingreso")
    void ingresoCorrecto() {
        Cliente cliente = clienteConPassword("Gamarra2026");
        when(cuentas.buscarPorEmail(TipoCuenta.CLIENTE, "ana@correo.com")).thenReturn(Optional.of(cliente));

        SesionEmitida sesion = servicio.iniciarSesion(TipoCuenta.CLIENTE, "  Ana@Correo.com ", "Gamarra2026");

        assertThat(sesion.cuenta()).isSameAs(cliente);
        assertThat(sesion.tokens().accessToken()).isEqualTo("acceso");
        assertThat(cliente.getUltimoLoginEn()).isEqualTo(AHORA);
        verify(intentos).limpiar(TipoCuenta.CLIENTE, "ana@correo.com");
        verify(intentos, never()).registrarFallo(any(), anyString());
    }

    @Test
    @DisplayName("CA-1-05 una contraseña errada cuenta como fallo y responde credenciales inválidas")
    void passwordErrada() {
        Cliente cliente = clienteConPassword("Gamarra2026");
        when(cuentas.buscarPorEmail(TipoCuenta.CLIENTE, "ana@correo.com")).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> servicio.iniciarSesion(TipoCuenta.CLIENTE, "ana@correo.com", "Otra12345"))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(intentos).registrarFallo(TipoCuenta.CLIENTE, "ana@correo.com");
        verify(tokens, never()).emitir(any());
    }

    @Test
    @DisplayName("CA-1-05 un correo inexistente también ejecuta BCrypt contra un hash ficticio (mismo tiempo de respuesta)")
    void correoInexistenteEjecutaBcrypt() {
        when(cuentas.buscarPorEmail(any(), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciarSesion(TipoCuenta.CLIENTE, "noexiste@correo.com", "Gamarra2026"))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(codificador).matches(org.mockito.ArgumentMatchers.eq("Gamarra2026"), anyString());
        verify(intentos).registrarFallo(TipoCuenta.CLIENTE, "noexiste@correo.com");
    }

    @Test
    @DisplayName("CA-1-05 una cuenta desactivada no ingresa aunque la contraseña sea correcta")
    void cuentaInactiva() {
        Cliente cliente = clienteConPassword("Gamarra2026");
        cliente.setActivo(false);
        when(cuentas.buscarPorEmail(TipoCuenta.CLIENTE, "ana@correo.com")).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> servicio.iniciarSesion(TipoCuenta.CLIENTE, "ana@correo.com", "Gamarra2026"))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(intentos).registrarFallo(TipoCuenta.CLIENTE, "ana@correo.com");
        assertThat(cliente.getUltimoLoginEn()).isNull();
    }

    @Test
    @DisplayName("CA-1-23 un staff con invitación pendiente (sin contraseña) no ingresa y también ejecuta BCrypt")
    void invitacionPendiente() {
        Staff pendiente = Datos.staff("nuevo@fitstore.pe", RolStaff.OPERADOR);
        pendiente.setId(UUID.randomUUID());
        when(cuentas.buscarPorEmail(TipoCuenta.STAFF, "nuevo@fitstore.pe")).thenReturn(Optional.of(pendiente));

        assertThatThrownBy(() -> servicio.iniciarSesion(TipoCuenta.STAFF, "nuevo@fitstore.pe", "Gamarra2026"))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(codificador).matches(org.mockito.ArgumentMatchers.eq("Gamarra2026"), anyString());
        verify(intentos).registrarFallo(TipoCuenta.STAFF, "nuevo@fitstore.pe");
    }

    @Test
    @DisplayName("CA-1-06 con la cuenta bloqueada responde 423 sin consultar la cuenta ni comparar la contraseña")
    void cuentaBloqueada() {
        when(intentos.estaBloqueado(TipoCuenta.CLIENTE, "ana@correo.com")).thenReturn(true);

        assertThatThrownBy(() -> servicio.iniciarSesion(TipoCuenta.CLIENTE, "ana@correo.com", "Gamarra2026"))
                .isInstanceOf(CuentaBloqueadaException.class);

        verify(cuentas, never()).buscarPorEmail(any(), anyString());
        verify(intentos, never()).registrarFallo(any(), anyString());
    }

    @Test
    @DisplayName("CA-1-24 el bloqueo se consulta por tipo de cuenta")
    void bloqueoPorTipo() {
        when(intentos.estaBloqueado(TipoCuenta.CLIENTE, "mismo@correo.com")).thenReturn(true);
        Staff staff = Datos.staff("mismo@correo.com", RolStaff.ADMIN);
        staff.setId(UUID.randomUUID());
        staff.setPasswordHash(codificador.encode("Gamarra2026"));
        when(cuentas.buscarPorEmail(TipoCuenta.STAFF, "mismo@correo.com")).thenReturn(Optional.of(staff));

        SesionEmitida sesion = servicio.iniciarSesion(TipoCuenta.STAFF, "mismo@correo.com", "Gamarra2026");

        assertThat(sesion.cuenta()).isSameAs(staff);
        verify(intentos).estaBloqueado(TipoCuenta.STAFF, "mismo@correo.com");
    }
}
