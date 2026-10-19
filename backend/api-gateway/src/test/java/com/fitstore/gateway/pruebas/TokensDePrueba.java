package com.fitstore.gateway.pruebas;

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

/** Claves RSA de prueba y tokens como los emitiría auth-service (claims sub, tipo, rol). */
public final class TokensDePrueba {

    private final RSAKey clave;
    private final NimbusJwtEncoder codificador;

    public TokensDePrueba(String kid) {
        try {
            this.clave = new RSAKeyGenerator(2048).keyID(kid).generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        this.codificador = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave)));
    }

    /** Escribe el JWKS público en un archivo temporal, como {@code infra/dev/jwks-prueba.json}. */
    public Path escribirJwks() {
        try {
            Path archivo = Files.createTempFile("jwks-prueba-", ".json");
            Files.writeString(archivo, new JWKSet(clave.toPublicJWK()).toString(), StandardCharsets.UTF_8);
            archivo.toFile().deleteOnExit();
            return archivo;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String token(String sub, String rol, Duration vigencia) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(sub)
                .claim("tipo", "CLIENTE".equals(rol) ? "CLIENTE" : "STAFF")
                .claim("rol", rol)
                .issuedAt(ahora.minus(Duration.ofMinutes(10)))
                .expiresAt(ahora.plus(vigencia))
                .build();
        JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256).keyId(clave.getKeyID()).build();
        return codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }
}
