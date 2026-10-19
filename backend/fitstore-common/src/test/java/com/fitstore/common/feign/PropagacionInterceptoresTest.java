package com.fitstore.common.feign;

import static org.assertj.core.api.Assertions.assertThat;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class PropagacionInterceptoresTest {

    @AfterEach
    void limpiar() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void peticionEntrante(String autorizacion, String requestId) {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        if (autorizacion != null) {
            peticion.addHeader("Authorization", autorizacion);
        }
        if (requestId != null) {
            peticion.addHeader("X-Request-Id", requestId);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(peticion));
    }

    @Test
    @DisplayName("Feign propaga el JWT del usuario a la llamada saliente")
    void propagaJwt() {
        peticionEntrante("Bearer abc.def.ghi", null);
        RequestTemplate plantilla = new RequestTemplate();

        new PropagacionJwtInterceptor().apply(plantilla);

        assertThat(plantilla.headers().get("Authorization")).containsExactly("Bearer abc.def.ghi");
    }

    @Test
    @DisplayName("Feign propaga el X-Request-Id de la petición entrante")
    void propagaRequestId() {
        peticionEntrante(null, "9f1c-uuid");
        RequestTemplate plantilla = new RequestTemplate();

        new PropagacionRequestIdInterceptor().apply(plantilla);

        assertThat(plantilla.headers().get("X-Request-Id")).containsExactly("9f1c-uuid");
    }

    @Test
    @DisplayName("Sin petición entrante o sin cabecera no agrega nada")
    void sinContexto() {
        RequestTemplate sinPeticion = new RequestTemplate();
        new PropagacionJwtInterceptor().apply(sinPeticion);
        new PropagacionRequestIdInterceptor().apply(sinPeticion);
        assertThat(sinPeticion.headers()).isEmpty();

        peticionEntrante(null, null);
        RequestTemplate sinCabeceras = new RequestTemplate();
        new PropagacionJwtInterceptor().apply(sinCabeceras);
        new PropagacionRequestIdInterceptor().apply(sinCabeceras);
        assertThat(sinCabeceras.headers()).isEmpty();
    }
}
