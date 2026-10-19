package com.fitstore.common.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Claves RSA de prueba y emisión de tokens como lo haría auth-service (claims sub, tipo, rol). */
final class TokensDePrueba {

    private final RSAKey clave;
    private final NimbusJwtEncoder codificador;

    TokensDePrueba(String kid) {
        try {
            this.clave = new RSAKeyGenerator(2048).keyID(kid).generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        this.codificador = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave)));
    }

    /** JWKS con solo la parte pública, como lo publica auth. */
    String jwksPublico() {
        return new JWKSet(clave.toPublicJWK()).toString();
    }

    Path escribirJwks() {
        try {
            Path archivo = Files.createTempFile("jwks-prueba-", ".json");
            Files.writeString(archivo, jwksPublico(), StandardCharsets.UTF_8);
            archivo.toFile().deleteOnExit();
            return archivo;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    String token(String sub, String tipo, String rol, Duration vigencia) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(sub)
                .claim("tipo", tipo)
                .claim("rol", rol)
                .issuedAt(ahora.minus(Duration.ofMinutes(10)))
                .expiresAt(ahora.plus(vigencia))
                .build();
        JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256).keyId(clave.getKeyID()).build();
        return codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }
}
