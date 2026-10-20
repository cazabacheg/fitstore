package com.fitstore.auth.controller;

import com.fitstore.auth.dto.response.ClaveJwkResponse;
import com.fitstore.auth.dto.response.JwksResponse;
import com.fitstore.auth.security.ProveedorClavesRsa;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Clave pública para validar los JWT: el gateway y los demás servicios la cachean (micro 7). */
@RestController
public class JwksController {

    private final ProveedorClavesRsa claves;

    public JwksController(ProveedorClavesRsa claves) {
        this.claves = claves;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "obtenerJwks", summary = "Clave pública (JWKS)")
    public ResponseEntity<JwksResponse> jwks() {
        List<ClaveJwkResponse> publicas = claves.jwkSet().getKeys().stream()
                .map(RSAKey.class::cast)
                .map(k -> new ClaveJwkResponse(k.getKeyType().getValue(), k.getKeyID(), k.getKeyUse().identifier(),
                        k.getAlgorithm().getName(), k.getModulus().toString(), k.getPublicExponent().toString()))
                .toList();
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600").body(new JwksResponse(publicas));
    }
}
