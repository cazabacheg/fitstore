package com.fitstore.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.auth.config.PropiedadesAuth;
import com.nimbusds.jose.jwk.RSAKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/** Par RSA de auth: se carga de archivos PEM montados, en dev se genera si faltan y en otro caso el arranque falla. */
class ProveedorClavesRsaTest {

    private static PropiedadesAuth con(String privada, String publica, boolean generar) {
        return new PropiedadesAuth("http://localhost:8080", Duration.ofMinutes(15), Duration.ofDays(7), 4,
                new PropiedadesAuth.Bloqueo(5, Duration.ofMinutes(15)), Duration.ofMinutes(30), Duration.ofHours(24),
                new PropiedadesAuth.Claves(privada, publica, "fs-pem", generar),
                new PropiedadesAuth.Cookie("fs_refresh", false, "Strict", "/api/v1/auth"),
                new PropiedadesAuth.Enlaces("http://tienda", "http://admin"));
    }

    private static String pem(String titulo, byte[] der) {
        return "-----BEGIN " + titulo + "-----\n" + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der)
                + "\n-----END " + titulo + "-----\n";
    }

    private static Jwt firmarYVerificar(ProveedorClavesRsa claves) {
        JwtClaimsSet datos = JwtClaimsSet.builder().subject("cuenta").build();
        String token = claves.jwtEncoder().encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId(claves.kid()).build(), datos)).getTokenValue();
        return claves.jwtDecoder().decode(token);
    }

    @Test
    @DisplayName("CA-1-36 carga el par RSA de los archivos PEM montados, firma con él y publica solo la parte pública con su kid")
    void cargaLosArchivosPem(@TempDir Path carpeta) throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair par = generador.generateKeyPair();
        Path privada = Files.writeString(carpeta.resolve("privada.pem"), pem("PRIVATE KEY", par.getPrivate().getEncoded()));
        Path publica = Files.writeString(carpeta.resolve("publica.pem"), pem("PUBLIC KEY", par.getPublic().getEncoded()));

        ProveedorClavesRsa claves = new ProveedorClavesRsa(con(privada.toString(), publica.toString(), false));

        assertThat(claves.kid()).isEqualTo("fs-pem");
        assertThat(firmarYVerificar(claves).getSubject()).isEqualTo("cuenta");
        RSAKey publicada = (RSAKey) claves.jwkSet().getKeys().get(0);
        assertThat(publicada.isPrivate()).isFalse();
        assertThat(publicada.toRSAPublicKey().getModulus()).isEqualTo(((java.security.interfaces.RSAPublicKey) par.getPublic()).getModulus());
    }

    @Test
    @DisplayName("CA-1-36 en dev, sin archivos, genera un par temporal que firma y valida")
    void generaUnParEnDev() {
        ProveedorClavesRsa claves = new ProveedorClavesRsa(con("${JWT_PRIVATE_KEY_PATH}", null, true));

        assertThat(firmarYVerificar(claves).getSubject()).isEqualTo("cuenta");
    }

    @Test
    @DisplayName("CA-1-36 fuera de dev, sin archivos de claves, falla al arrancar")
    void fallaSinArchivosFueraDeDev() {
        assertThatThrownBy(() -> new ProveedorClavesRsa(con("/no/existe/privada.pem", "/no/existe/publica.pem", false)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("JWT_PRIVATE_KEY_PATH");
    }

    @Test
    @DisplayName("CA-1-36 un archivo PEM ilegible hace fallar el arranque")
    void pemInvalido(@TempDir Path carpeta) throws Exception {
        Path privada = Files.writeString(carpeta.resolve("privada.pem"), pem("PRIVATE KEY", "no soy una clave".getBytes()));
        Path publica = Files.writeString(carpeta.resolve("publica.pem"), pem("PUBLIC KEY", "tampoco".getBytes()));

        assertThatThrownBy(() -> new ProveedorClavesRsa(con(privada.toString(), publica.toString(), true)))
                .isInstanceOf(IllegalStateException.class);
    }
}
