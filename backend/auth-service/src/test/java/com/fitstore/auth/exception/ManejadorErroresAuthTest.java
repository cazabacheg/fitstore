package com.fitstore.auth.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.common.api.ApiError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/** Errores propios de auth con el formato K5. */
class ManejadorErroresAuthTest {

    private final ManejadorErroresAuth manejador = new ManejadorErroresAuth();

    @Test
    @DisplayName("CA-1-27 un choque de versión (bloqueo optimista) responde 409 CONFLICTO_VERSION")
    void conflictoDeVersion() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("X-Request-Id", "9d1b6a5e-6a62-4ff6-9a73-0a4e0bb7f0a1");

        ResponseEntity<ApiError> respuesta = manejador.alConflictoDeVersion(new OptimisticLockingFailureException("choque"), peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().codigo()).isEqualTo("CONFLICTO_VERSION");
        assertThat(respuesta.getBody().requestId()).isEqualTo("9d1b6a5e-6a62-4ff6-9a73-0a4e0bb7f0a1");
        assertThat(respuesta.getBody().success()).isFalse();
    }
}
