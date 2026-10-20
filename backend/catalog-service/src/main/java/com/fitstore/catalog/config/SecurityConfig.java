package com.fitstore.catalog.config;

import com.fitstore.common.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seguridad de catalog (micro 8). La base de {@code fitstore-common} deja todo cerrado; aquí se abren solo los
 * endpoints públicos de la tienda (lectura del catálogo y la cotización). El resto exige JWT y cada operación
 * restringe su rol con {@code @PreAuthorize}. {@code /internal/**} exige un JWT válido y el gateway no lo enruta.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    private static final String SIN_SESION = "Tu sesión venció, vuelve a ingresar";
    private static final String SIN_PERMISO = "No tienes permiso para esta acción";

    @Bean
    SecurityFilterChain catalogSecurityFilterChain(HttpSecurity http, JsonMapper json, JwtAuthenticationConverter convertidor)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**",
                                "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categorias", "/api/v1/categorias/*/guia-tallas",
                                "/api/v1/productos", "/api/v1/productos/*", "/api/v1/tallas", "/api/v1/colores").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/precios/cotizar").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(convertidor))
                        .authenticationEntryPoint((peticion, respuesta, e) -> escribir(json, peticion, respuesta,
                                HttpStatus.UNAUTHORIZED, ApiError.de(SIN_SESION, "NO_AUTENTICADO")))
                        .accessDeniedHandler((peticion, respuesta, e) -> escribir(json, peticion, respuesta,
                                HttpStatus.FORBIDDEN, ApiError.de(SIN_PERMISO, "SIN_PERMISO"))))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((peticion, respuesta, ex) -> escribir(json, peticion, respuesta,
                                HttpStatus.UNAUTHORIZED, ApiError.de(SIN_SESION, "NO_AUTENTICADO")))
                        .accessDeniedHandler((peticion, respuesta, ex) -> escribir(json, peticion, respuesta,
                                HttpStatus.FORBIDDEN, ApiError.de(SIN_PERMISO, "SIN_PERMISO"))))
                .build();
    }

    private static void escribir(JsonMapper json, HttpServletRequest peticion, HttpServletResponse respuesta,
            HttpStatus estado, ApiError error) throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        json.writeValue(respuesta.getOutputStream(), error.conRequestId(peticion.getHeader("X-Request-Id")));
    }
}
