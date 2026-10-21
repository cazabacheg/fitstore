package com.fitstore.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.auth.config.PropiedadesAuth;
import com.fitstore.auth.exception.SesionInvalidaException;
import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.pruebas.PruebaApi;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** Emisión, rotación, reutilización e invalidación por fecha de los tokens, con Redis y PostgreSQL reales. */
class TokenServiceTest extends PruebaApi {

    @Autowired TokenService tokens;
    @Autowired ProveedorClavesRsa claves;
    @Autowired PropiedadesAuth propiedades;
    @Autowired RegistroRefreshRevocados revocados;
    @Autowired CuentaLookup cuentas;
    @Autowired JwtDecoder decoderDeAuth;

    @Test
    @DisplayName("CA-1-36 el access token lleva sub, tipo, rol, email y nombre, y lo valida la clave pública del JWKS")
    void accessTokenConSusClaims() {
        Staff admin = crearStaff("admin@fitstore.pe", RolStaff.ADMIN);

        ParTokens par = tokens.emitir(admin);
        Jwt jwt = decoderDeAuth.decode(par.accessToken());

        assertThat(jwt.getSubject()).isEqualTo(admin.getId().toString());
        assertThat(jwt.<String>getClaim("tipo")).isEqualTo("STAFF");
        assertThat(jwt.<String>getClaim("rol")).isEqualTo("ADMIN");
        assertThat(jwt.<String>getClaim("email")).isEqualTo("admin@fitstore.pe");
        assertThat(jwt.<String>getClaim("nombre")).isEqualTo("Marco Rojas");
        assertThat(jwt.getHeaders()).containsEntry("kid", "fs-prueba").containsEntry("alg", "RS256");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("CA-1-36 un refresh token no sirve como access token en los endpoints protegidos")
    void refreshNoEsAccessToken() {
        Cliente cliente = crearCliente("ana@correo.com", "45678912");

        ParTokens par = tokens.emitir(cliente);

        assertThatThrownBy(() -> decoderDeAuth.decode(par.refreshToken())).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("CA-1-08 refrescar entrega un par nuevo para la misma cuenta y revoca el refresh usado (rotación)")
    void refrescarRota() {
        Cliente cliente = crearCliente("ana@correo.com", "45678912");
        ParTokens original = tokens.emitir(cliente);

        SesionEmitida nueva = tokens.refrescar(original.refreshToken());

        assertThat(nueva.cuenta().getId()).isEqualTo(cliente.getId());
        assertThat(nueva.tokens().refreshToken()).isNotEqualTo(original.refreshToken());
        assertThat(nueva.tokens().accessToken()).isNotBlank();
        List<String> revocadas = redis.keys("auth:refresh:revocado:*").stream().toList();
        assertThat(revocadas).hasSize(1);
        assertThat(redis.getExpire(revocadas.get(0))).isBetween(1L, Duration.ofDays(7).toSeconds());
    }

    @Test
    @DisplayName("CA-1-08 un refresh de staff se busca en la tabla de staff")
    void refrescarStaff() {
        Staff staff = crearStaff("op@fitstore.pe", RolStaff.OPERADOR);

        SesionEmitida nueva = tokens.refrescar(tokens.emitir(staff).refreshToken());

        assertThat(nueva.cuenta()).isInstanceOf(Staff.class);
        assertThat(nueva.cuenta().getId()).isEqualTo(staff.getId());
    }

    @Test
    @DisplayName("CA-1-09 un refresh ya usado no se puede usar de nuevo")
    void refreshReutilizado() {
        Cliente cliente = crearCliente("ana@correo.com", "45678912");
        ParTokens original = tokens.emitir(cliente);
        tokens.refrescar(original.refreshToken());

        assertThatThrownBy(() -> tokens.refrescar(original.refreshToken())).isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    @DisplayName("CA-1-10 revocar deja el refresh inservible e ignora un token ilegible")
    void revocar() {
        Cliente cliente = crearCliente("ana@correo.com", "45678912");
        ParTokens par = tokens.emitir(cliente);

        tokens.revocar(par.refreshToken());

        assertThatThrownBy(() -> tokens.refrescar(par.refreshToken())).isInstanceOf(SesionInvalidaException.class);
        assertThatCode(() -> tokens.revocar("basura")).doesNotThrowAnyException();
        assertThatCode(() -> tokens.revocar(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CA-1-11 un refresh emitido antes de cambiar las credenciales se rechaza; uno posterior sirve")
    void invalidacionPorFecha() {
        Cliente cliente = crearCliente("ana@correo.com", "45678912");
        ParTokens anterior = tokens.emitir(cliente);

        cliente.setCredencialesActualizadasEn(cliente.getCredencialesActualizadasEn().plusMillis(5));
        clientes.saveAndFlush(cliente);

        assertThatThrownBy(() -> tokens.refrescar(anterior.refreshToken())).isInstanceOf(SesionInvalidaException.class);
        assertThat(tokens.refrescar(tokens.emitir(cliente).refreshToken()).cuenta().getId()).isEqualTo(cliente.getId());
    }

    @Test
    @DisplayName("CA-1-29 una cuenta desactivada no puede refrescar su sesión")
    void cuentaInactivaNoRefresca() {
        Cliente cliente = crearCliente("ana@correo.com", "45678912");
        ParTokens par = tokens.emitir(cliente);
        cliente.setActivo(false);
        clientes.saveAndFlush(cliente);

        assertThatThrownBy(() -> tokens.refrescar(par.refreshToken())).isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    @DisplayName("CA-1-08 un refresh de una cuenta que ya no existe se rechaza")
    void cuentaInexistente() {
        Cliente cliente = crearCliente("ana@correo.com", "45678912");
        ParTokens par = tokens.emitir(cliente);
        jdbc.execute("TRUNCATE TABLE consentimiento, cliente CASCADE");

        assertThatThrownBy(() -> tokens.refrescar(par.refreshToken())).isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    @DisplayName("CA-1-08 un refresh vencido, un access token, un token ajeno y la basura se rechazan")
    void tokensInvalidos() {
        Cliente cliente = crearCliente("ana@correo.com", "45678912");
        Clock hace8Dias = Clock.fixed(Instant.now().minus(Duration.ofDays(8)), ZoneOffset.UTC);
        TokenService delPasado = new TokenService(claves, propiedades, hace8Dias, revocados, cuentas);
        PropiedadesAuth otroEmisor = propiedades;
        ProveedorClavesRsa otraClave = new ProveedorClavesRsa(otroEmisor);
        TokenService ajeno = new TokenService(otraClave, propiedades, Clock.systemUTC(), revocados, cuentas);

        assertThatThrownBy(() -> tokens.refrescar(delPasado.emitir(cliente).refreshToken())).isInstanceOf(SesionInvalidaException.class);
        assertThatThrownBy(() -> tokens.refrescar(tokens.emitir(cliente).accessToken())).isInstanceOf(SesionInvalidaException.class);
        assertThatThrownBy(() -> tokens.refrescar(ajeno.emitir(cliente).refreshToken())).isInstanceOf(SesionInvalidaException.class);
        assertThatThrownBy(() -> tokens.refrescar("basura")).isInstanceOf(SesionInvalidaException.class);
        assertThatThrownBy(() -> tokens.refrescar(null)).isInstanceOf(SesionInvalidaException.class);
    }
}
