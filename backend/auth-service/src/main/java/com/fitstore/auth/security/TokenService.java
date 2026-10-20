package com.fitstore.auth.security;

import com.fitstore.auth.config.PropiedadesAuth;
import com.fitstore.auth.exception.SesionInvalidaException;
import com.fitstore.auth.model.Cuenta;
import com.fitstore.auth.model.TipoCuenta;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

/**
 * Emisión de los JWT RS256 (micro 4.4): access token de 15 min con {@code sub}, {@code tipo}, {@code rol}, {@code email}
 * y {@code nombre} (nunca el DNI), y refresh token de 7 días con {@code typ = refresh}.
 */
@Service
public class TokenService {

    private static final Logger LOG = LoggerFactory.getLogger(TokenService.class);

    /** Claim que distingue al refresh token del access token. */
    static final String CLAIM_TIPO_TOKEN = "typ";
    static final String VALOR_REFRESH = "refresh";
    /** Marca de {@code credenciales_actualizadas_en} (ms) al emitir el refresh; ver {@code refrescar}. */
    static final String CLAIM_CREDENCIALES = "cred";

    private final JwtEncoder encoder;
    private final String kid;
    private final PropiedadesAuth propiedades;
    private final Clock reloj;
    private final NimbusJwtDecoder decoder;
    private final RegistroRefreshRevocados revocados;
    private final CuentaLookup cuentas;

    public TokenService(ProveedorClavesRsa claves, PropiedadesAuth propiedades, Clock reloj,
            RegistroRefreshRevocados revocados, CuentaLookup cuentas) {
        this.encoder = claves.jwtEncoder();
        this.kid = claves.kid();
        this.propiedades = propiedades;
        this.reloj = reloj;
        this.revocados = revocados;
        this.cuentas = cuentas;
        this.decoder = claves.jwtDecoder();
        JwtTimestampValidator vigencia = new JwtTimestampValidator(Duration.ZERO);
        vigencia.setClock(reloj);
        this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(vigencia, new JwtIssuerValidator(propiedades.issuer())));
    }

    /**
     * Refresco (micro 10.2): verifica firma y vigencia y exige {@code typ = refresh}; un {@code jti} ya revocado es un
     * posible robo; la cuenta debe existir, estar activa y no haber cambiado sus credenciales después de emitirse el
     * token. Revoca el {@code jti} usado y emite un par nuevo (rotación).
     */
    public SesionEmitida refrescar(String refreshToken) {
        Jwt jwt = decodificarRefresh(refreshToken);
        if (revocados.estaRevocado(jwt.getId())) {
            LOG.warn("Posible robo de sesión: se usó un refresh token ya revocado (cuenta {}, jti {})", jwt.getSubject(), jwt.getId());
            throw new SesionInvalidaException();
        }
        Cuenta cuenta = buscarCuenta(jwt);
        Long marca = jwt.getClaim(CLAIM_CREDENCIALES);
        if (!cuenta.isActivo() || marca == null || marca < cuenta.getCredencialesActualizadasEn().toEpochMilli()) {
            throw new SesionInvalidaException();
        }
        if (!revocados.revocar(jwt.getId(), vidaRestante(jwt))) {
            LOG.warn("Posible robo de sesión: dos usos simultáneos del refresh token {} (cuenta {})", jwt.getId(), jwt.getSubject());
            throw new SesionInvalidaException();
        }
        return new SesionEmitida(cuenta, emitir(cuenta));
    }

    /** Revoca el refresh token (logout). Si ya no es válido no hace nada. */
    public void revocar(String refreshToken) {
        try {
            Jwt jwt = decodificarRefresh(refreshToken);
            revocados.revocar(jwt.getId(), vidaRestante(jwt));
        } catch (SesionInvalidaException e) {
            LOG.debug("Logout con un refresh token que ya no es válido");
        }
    }

    private Jwt decodificarRefresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new SesionInvalidaException();
        }
        try {
            Jwt jwt = decoder.decode(refreshToken);
            if (!VALOR_REFRESH.equals(jwt.getClaimAsString(CLAIM_TIPO_TOKEN)) || jwt.getId() == null) {
                throw new SesionInvalidaException();
            }
            return jwt;
        } catch (JwtException | IllegalArgumentException e) {
            throw new SesionInvalidaException();
        }
    }

    private Cuenta buscarCuenta(Jwt jwt) {
        try {
            TipoCuenta tipo = TipoCuenta.valueOf(jwt.getClaimAsString("tipo"));
            return cuentas.buscar(tipo, UUID.fromString(jwt.getSubject())).orElseThrow(SesionInvalidaException::new);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new SesionInvalidaException();
        }
    }

    private Duration vidaRestante(Jwt jwt) {
        return Duration.between(reloj.instant(), jwt.getExpiresAt());
    }

    /** Emite un par nuevo de tokens para la cuenta. */
    public ParTokens emitir(Cuenta cuenta) {
        Instant ahora = reloj.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant accessExpira = ahora.plus(propiedades.accessTtl());
        Instant refreshExpira = ahora.plus(propiedades.refreshTtl());

        JwtClaimsSet access = JwtClaimsSet.builder()
                .issuer(propiedades.issuer())
                .subject(cuenta.getId().toString())
                .issuedAt(ahora)
                .expiresAt(accessExpira)
                .id(UUID.randomUUID().toString())
                .claim("tipo", cuenta.tipo().name())
                .claim("rol", cuenta.rol())
                .claim("email", cuenta.getEmail())
                .claim("nombre", cuenta.getNombres() + " " + cuenta.getApellidos())
                .build();
        JwtClaimsSet refresh = JwtClaimsSet.builder()
                .issuer(propiedades.issuer())
                .subject(cuenta.getId().toString())
                .issuedAt(ahora)
                .expiresAt(refreshExpira)
                .id(UUID.randomUUID().toString())
                .claim("tipo", cuenta.tipo().name())
                .claim(CLAIM_TIPO_TOKEN, VALOR_REFRESH)
                .claim(CLAIM_CREDENCIALES, cuenta.getCredencialesActualizadasEn().toEpochMilli())
                .build();
        return new ParTokens(firmar(access), accessExpira, firmar(refresh), refreshExpira);
    }

    private String firmar(JwtClaimsSet claims) {
        JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256).keyId(kid).build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }
}
