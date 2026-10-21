package com.fitstore.auth.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fitstore.auth.config.PropiedadesAuth;
import com.fitstore.auth.exception.SesionInvalidaException;
import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.TipoCuenta;
import com.fitstore.auth.pruebas.Datos;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/** Casos del refresh que no se alcanzan con tokens emitidos normalmente (dos usos simultáneos y claims ajenos). */
class TokenServiceConcurrenciaTest {

    private final PropiedadesAuth propiedades = new PropiedadesAuth("http://localhost:8080", Duration.ofMinutes(15),
            Duration.ofDays(7), 4, new PropiedadesAuth.Bloqueo(5, Duration.ofMinutes(15)), Duration.ofMinutes(30),
            Duration.ofHours(24), new PropiedadesAuth.Claves(null, null, "fs-test", true),
            new PropiedadesAuth.Cookie("fs_refresh", false, "Strict", "/api/v1/auth"),
            new PropiedadesAuth.Enlaces("http://tienda", "http://admin"));
    private final ProveedorClavesRsa claves = new ProveedorClavesRsa(propiedades);
    private final RegistroRefreshRevocados revocados = mock(RegistroRefreshRevocados.class);
    private final CuentaLookup cuentas = mock(CuentaLookup.class);
    private final TokenService tokens = new TokenService(claves, propiedades, Clock.systemUTC(), revocados, cuentas);

    private Cliente cliente() {
        Cliente c = Datos.cliente("ana@correo.com", "45678912");
        c.setId(UUID.randomUUID());
        return c;
    }

    @Test
    @DisplayName("CA-1-09 dos usos simultáneos del mismo refresh: solo uno gana la revocación y el otro responde 401")
    void dosUsosSimultaneos() {
        Cliente cliente = cliente();
        when(cuentas.buscar(TipoCuenta.CLIENTE, cliente.getId())).thenReturn(Optional.of(cliente));
        when(revocados.estaRevocado(any())).thenReturn(false);
        when(revocados.revocar(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> tokens.refrescar(tokens.emitir(cliente).refreshToken())).isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    @DisplayName("CA-1-08 un refresh firmado por auth pero con un tipo de cuenta desconocido se rechaza")
    void tipoDeCuentaDesconocido() {
        JwtClaimsSet datos = JwtClaimsSet.builder().issuer(propiedades.issuer()).subject(UUID.randomUUID().toString())
                .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(60)).id("x")
                .claim("tipo", "EXTRATERRESTRE").claim("typ", "refresh").claim("cred", 0L).build();
        String refresh = claves.jwtEncoder().encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId("fs-test").build(), datos)).getTokenValue();

        assertThatThrownBy(() -> tokens.refrescar(refresh)).isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    @DisplayName("CA-1-08 un refresh sin la marca de credenciales se rechaza")
    void sinMarcaDeCredenciales() {
        Cliente cliente = cliente();
        when(cuentas.buscar(TipoCuenta.CLIENTE, cliente.getId())).thenReturn(Optional.of(cliente));
        JwtClaimsSet datos = JwtClaimsSet.builder().issuer(propiedades.issuer()).subject(cliente.getId().toString())
                .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(60)).id("y")
                .claim("tipo", "CLIENTE").claim("typ", "refresh").build();
        String refresh = claves.jwtEncoder().encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId("fs-test").build(), datos)).getTokenValue();

        assertThatThrownBy(() -> tokens.refrescar(refresh)).isInstanceOf(SesionInvalidaException.class);
    }
}
