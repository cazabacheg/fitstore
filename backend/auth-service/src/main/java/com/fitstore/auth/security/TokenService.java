package com.fitstore.auth.security;

import com.fitstore.auth.config.PropiedadesAuth;
import com.fitstore.auth.model.Cuenta;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emisión de los JWT RS256 (micro 4.4): access token de 15 min con {@code sub}, {@code tipo}, {@code rol}, {@code email}
 * y {@code nombre} (nunca el DNI), y refresh token de 7 días con {@code typ = refresh}.
 */
@Service
public class TokenService {

    /** Claim que distingue al refresh token del access token. */
    static final String CLAIM_TIPO_TOKEN = "typ";
    static final String VALOR_REFRESH = "refresh";
    /** Marca de {@code credenciales_actualizadas_en} (ms) al emitir el refresh; ver {@code refrescar}. */
    static final String CLAIM_CREDENCIALES = "cred";

    private final JwtEncoder encoder;
    private final String kid;
    private final PropiedadesAuth propiedades;
    private final Clock reloj;

    public TokenService(ProveedorClavesRsa claves, PropiedadesAuth propiedades, Clock reloj) {
        this.encoder = claves.jwtEncoder();
        this.kid = claves.kid();
        this.propiedades = propiedades;
        this.reloj = reloj;
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
