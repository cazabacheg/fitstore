package com.fitstore.delivery.config;

import com.fitstore.common.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.core.convert.converter.Converter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seguridad de delivery: las opciones, la cotización y las agencias activas del checkout no piden sesión; todo lo demás
 * sí, y cada controlador de {@code /admin} fija su rol con {@code @PreAuthorize} (OPERADOR: entregas y repartidores;
 * ADMIN: además tarifas y agencias).
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain deliverySecurityFilterChain(HttpSecurity http, JsonMapper json,
            JwtAuthenticationConverter convertidor) throws Exception {
        RespuestasSeguridad respuestas = new RespuestasSeguridad(json);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**",
                                "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/envios/opciones", "/api/v1/agencias").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/envios/cotizar").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(
                                (Converter<Jwt, ? extends AbstractAuthenticationToken>) convertidor))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .exceptionHandling(e -> e.authenticationEntryPoint(respuestas).accessDeniedHandler(respuestas))
                .build();
    }

    /** 401 y 403 con el formato de error común (K5). */
    static class RespuestasSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

        private final JsonMapper json;

        RespuestasSeguridad(JsonMapper json) {
            this.json = json;
        }

        @Override
        public void commence(HttpServletRequest peticion, HttpServletResponse respuesta, AuthenticationException e)
                throws IOException {
            escribir(peticion, respuesta, HttpStatus.UNAUTHORIZED,
                    ApiError.de("Tu sesión venció, vuelve a ingresar", "NO_AUTENTICADO"));
        }

        @Override
        public void handle(HttpServletRequest peticion, HttpServletResponse respuesta, AccessDeniedException e)
                throws IOException {
            escribir(peticion, respuesta, HttpStatus.FORBIDDEN,
                    ApiError.de("No tienes permiso para esta acción", "SIN_PERMISO"));
        }

        private void escribir(HttpServletRequest peticion, HttpServletResponse respuesta, HttpStatus estado,
                ApiError error) throws IOException {
            respuesta.setStatus(estado.value());
            respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
            respuesta.setCharacterEncoding("UTF-8");
            json.writeValue(respuesta.getOutputStream(), error.conRequestId(peticion.getHeader("X-Request-Id")));
        }
    }
}
