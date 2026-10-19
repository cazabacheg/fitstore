package com.fitstore.common.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Valid;
import java.util.Set;
import org.mockito.Mockito;
import jakarta.validation.constraints.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Formato de error K5 para cada tipo de excepción. */
class GlobalExceptionHandlerBaseTest {

    record Cuerpo(@Pattern(regexp = "[0-9]{8}", message = "El DNI debe tener 8 dígitos") String dni) {}

    @RestController
    static class ControladorDePrueba {
        @GetMapping("/no-encontrado")
        void noEncontrado() { throw new RecursoNoEncontradoException("No encontramos el pedido"); }

        @GetMapping("/conflicto")
        void conflicto() { throw new ConflictoNegocioException("CORREO_DUPLICADO", "Ese correo ya está registrado"); }

        @GetMapping("/prohibido")
        void prohibido() { throw new AccessDeniedException("no"); }

        @GetMapping("/sin-sesion")
        void sinSesion() { throw new BadCredentialsException("no"); }

        @GetMapping("/error")
        void error() { throw new IllegalStateException("detalle interno que no se debe exponer"); }

        @GetMapping("/restricciones")
        void restricciones() {
            ConstraintViolation<?> sobreElDni = violacion("crear.cuerpo.dni", "El DNI debe tener 8 dígitos");
            ConstraintViolation<?> sinPunto = violacion("celular", "El celular debe empezar con 9");
            throw new ConstraintViolationException(Set.of(sobreElDni, sinPunto));
        }

        @PostMapping("/validar")
        void validar(@Valid @RequestBody Cuerpo cuerpo) { }
    }

    private static ConstraintViolation<?> violacion(String ruta, String mensaje) {
        ConstraintViolation<?> v = Mockito.mock(ConstraintViolation.class);
        Path path = Mockito.mock(Path.class);
        Mockito.when(path.toString()).thenReturn(ruta);
        Mockito.when(v.getPropertyPath()).thenReturn(path);
        Mockito.when(v.getMessage()).thenReturn(mensaje);
        return v;
    }

    @RestControllerAdvice
    static class Manejador extends GlobalExceptionHandlerBase { }

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        var validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new Manejador())
                .setValidator(validador)
                .build();
    }

    @Test
    @DisplayName("K5 recurso no encontrado responde 404 NO_ENCONTRADO")
    void noEncontrado() throws Exception {
        mvc.perform(get("/no-encontrado").header("X-Request-Id", "abc-123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("No encontramos el pedido"))
                .andExpect(jsonPath("$.requestId").value("abc-123"));
    }

    @Test
    @DisplayName("K5 conflicto de negocio responde 409 con el código propio")
    void conflicto() throws Exception {
        mvc.perform(get("/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_DUPLICADO"));
    }

    @Test
    @DisplayName("K5 validación responde 400 VALIDACION con un error por campo")
    void validacion() throws Exception {
        mvc.perform(post("/validar").contentType(MediaType.APPLICATION_JSON).content("{\"dni\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores[0].campo").value("dni"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("El DNI debe tener 8 dígitos"));
    }

    @Test
    @DisplayName("K5 cuerpo ilegible responde 400 VALIDACION")
    void cuerpoIlegible() throws Exception {
        mvc.perform(post("/validar").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    @DisplayName("K5 acceso denegado responde 403 SIN_PERMISO")
    void sinPermiso() throws Exception {
        mvc.perform(get("/prohibido"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    @Test
    @DisplayName("K5 autenticación fallida responde 401 NO_AUTENTICADO")
    void sinSesion() throws Exception {
        mvc.perform(get("/sin-sesion"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("K5 error inesperado responde 500 sin exponer el detalle interno")
    void errorInesperado() throws Exception {
        mvc.perform(get("/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error inesperado, inténtalo en un momento"));
    }

    @Test
    @DisplayName("K5 ruta inexistente responde 404 NO_ENCONTRADO")
    void rutaInexistente() throws Exception {
        mvc.perform(get("/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("K5 ConstraintViolationException responde 400 VALIDACION con un error por campo, recortado al último nodo")
    void violacionDeRestricciones() throws Exception {
        mvc.perform(get("/restricciones"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores.length()").value(2))
                .andExpect(jsonPath("$.errores[?(@.campo=='dni')].mensaje").value("El DNI debe tener 8 dígitos"))
                .andExpect(jsonPath("$.errores[?(@.campo=='celular')].mensaje").value("El celular debe empezar con 9"));
    }
}
