package com.fitstore.auth.security;

import com.fitstore.auth.config.PropiedadesAuth;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Par RSA con el que auth firma los JWT (RS256). Se carga de archivos PEM montados por variables de entorno
 * (nunca van en el repo); en el perfil dev, si faltan, se genera uno al arrancar. El JWKS publica el {@code kid}
 * para poder rotar claves en el futuro.
 */
@Component
public class ProveedorClavesRsa {

    private static final Logger LOG = LoggerFactory.getLogger(ProveedorClavesRsa.class);

    private final RSAKey clave;

    public ProveedorClavesRsa(PropiedadesAuth propiedades) {
        this.clave = cargar(propiedades.claves());
    }

    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave)));
    }

    /** Decodificador con la clave pública propia; no valida emisor ni vigencia: eso lo hace quien lo usa. */
    public NimbusJwtDecoder jwtDecoder() {
        try {
            return NimbusJwtDecoder.withPublicKey(clave.toRSAPublicKey()).build();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo construir el decodificador JWT", e);
        }
    }

    /** Solo la parte pública (lo que publica {@code /.well-known/jwks.json}). */
    public JWKSet jwkSet() {
        return new JWKSet(clave).toPublicJWKSet();
    }

    public String kid() {
        return clave.getKeyID();
    }

    private static RSAKey cargar(PropiedadesAuth.Claves claves) {
        try {
            if (existe(claves.privada()) && existe(claves.publica())) {
                RSAPrivateKey privada = leerPrivada(Path.of(claves.privada()));
                RSAPublicKey publica = leerPublica(Path.of(claves.publica()));
                return construir(publica, privada, claves.kid());
            }
            if (!claves.generarSiFaltan()) {
                throw new IllegalStateException(
                        "Faltan las claves JWT: define JWT_PRIVATE_KEY_PATH y JWT_PUBLIC_KEY_PATH con archivos PEM existentes");
            }
            LOG.warn("No hay archivos de claves JWT: se genera un par RSA temporal (solo desarrollo; los tokens no sobreviven al reinicio)");
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            KeyPair par = generador.generateKeyPair();
            return construir((RSAPublicKey) par.getPublic(), (RSAPrivateKey) par.getPrivate(), claves.kid());
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("No se pudieron cargar las claves JWT", e);
        }
    }

    private static RSAKey construir(RSAPublicKey publica, RSAPrivateKey privada, String kid) {
        return new RSAKey.Builder(publica)
                .privateKey(privada)
                .keyID(kid)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build();
    }

    private static boolean existe(String ruta) {
        return StringUtils.hasText(ruta) && Files.isReadable(Path.of(ruta));
    }

    private static RSAPrivateKey leerPrivada(Path archivo) throws IOException, GeneralSecurityException {
        byte[] der = pem(archivo);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static RSAPublicKey leerPublica(Path archivo) throws IOException, GeneralSecurityException {
        byte[] der = pem(archivo);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    private static byte[] pem(Path archivo) throws IOException {
        String contenido = Files.readString(archivo)
                .replaceAll("-----(BEGIN|END)[A-Z ]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(contenido);
    }
}
