package com.fitstore.auth.config;

import com.fitstore.auth.security.ProveedorClavesRsa;
import com.fitstore.auth.security.RespuestasSeguridadAuth;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seguridad de auth: endpoints públicos (registro, logins, refresh, logout, olvido, restablecer y JWKS) y todo lo
 * demás con JWT propio. Los roles se aplican con {@code SoloCliente}, {@code SoloStaff} y {@code SoloAdmin}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropiedadesAuth.class)
public class SecurityConfig {

    private static final String[] PUBLICOS_POST = {
        "/api/v1/auth/registro", "/api/v1/auth/login", "/api/v1/auth/staff/login", "/api/v1/auth/refresh",
        "/api/v1/auth/logout", "/api/v1/auth/password/olvido", "/api/v1/auth/staff/password/olvido",
        "/api/v1/auth/password/restablecer"
    };

    private static final String[] PUBLICOS_GET = {
        "/.well-known/jwks.json", "/actuator/health", "/actuator/health/**",
        "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
    };

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordEncoder passwordEncoder(PropiedadesAuth propiedades) {
        return new BCryptPasswordEncoder(propiedades.bcryptStrength());
    }

    /**
     * Valida los access tokens con la clave pública propia (sin llamarse a sí mismo por HTTP): firma, emisor y
     * vigencia exacta; un refresh token ({@code typ = refresh}) nunca sirve como access token.
     */
    @Bean
    JwtDecoder jwtDecoder(ProveedorClavesRsa claves, PropiedadesAuth propiedades) {
        NimbusJwtDecoder decoder = claves.jwtDecoder();
        OAuth2TokenValidator<Jwt> noEsRefresh = jwt -> "refresh".equals(jwt.getClaimAsString("typ"))
                ? OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Un refresh token no es un access token", null))
                : OAuth2TokenValidatorResult.success();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO),
                new JwtIssuerValidator(propiedades.issuer()),
                noEsRefresh));
        return decoder;
    }

    /** Ignora el encabezado Authorization en los endpoints públicos de sesión: un access token vencido no debe impedir ingresar ni refrescar. */
    @Bean
    BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver porDefecto = new DefaultBearerTokenResolver();
        return peticion -> {
            String ruta = peticion.getRequestURI();
            return ruta.startsWith("/api/v1/auth/") || ruta.startsWith("/.well-known/") ? null : porDefecto.resolve(peticion);
        };
    }

    @Bean
    SecurityFilterChain authSecurityFilterChain(HttpSecurity http, JsonMapper json, JwtAuthenticationConverter convertidor,
            BearerTokenResolver resolver, RoleHierarchy jerarquia) throws Exception {
        RespuestasSeguridadAuth respuestas = new RespuestasSeguridadAuth(json);
        // El rol y el tipo de cuenta se exigen primero por ruta (antes de leer y validar el cuerpo, así un 403 nunca
        // se convierte en un 400) y otra vez en cada endpoint con SoloCliente, SoloStaff y SoloAdmin.
        AuthorizationManager<RequestAuthorizationContext> cliente = sesion("CLIENTE", "CLIENTE", jerarquia);
        AuthorizationManager<RequestAuthorizationContext> staff = sesion("STAFF", "OPERADOR", jerarquia);
        AuthorizationManager<RequestAuthorizationContext> admin = sesion("STAFF", "ADMIN", jerarquia);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, PUBLICOS_POST).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLICOS_GET).permitAll()
                        .requestMatchers("/api/v1/clientes/me", "/api/v1/clientes/me/**").access(cliente)
                        .requestMatchers("/api/v1/staff/me", "/api/v1/staff/me/**").access(staff)
                        .requestMatchers("/api/v1/staff", "/api/v1/staff/**").access(admin)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/clientes/*/desactivar", "/api/v1/clientes/*/activar").access(admin)
                        .requestMatchers("/api/v1/clientes", "/api/v1/clientes/**").access(staff)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .bearerTokenResolver(resolver)
                        .jwt(j -> j.jwtAuthenticationConverter((Converter<Jwt, ? extends AbstractAuthenticationToken>) convertidor))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .exceptionHandling(e -> e.authenticationEntryPoint(respuestas).accessDeniedHandler(respuestas))
                .build();
    }

    /** Autoriza solo una sesión del {@code tipo} de cuenta indicado cuyo rol alcance al pedido (ADMIN alcanza a OPERADOR). */
    private static AuthorizationManager<RequestAuthorizationContext> sesion(String tipo, String rol, RoleHierarchy jerarquia) {
        return (autenticacion, contexto) -> {
            Authentication actual = autenticacion.get();
            boolean permitido = actual instanceof JwtAuthenticationToken jwt
                    && actual.isAuthenticated()
                    && tipo.equals(jwt.getToken().getClaimAsString("tipo"))
                    && jerarquia.getReachableGrantedAuthorities(actual.getAuthorities()).stream()
                            .anyMatch(autoridad -> ("ROLE_" + rol).equals(autoridad.getAuthority()));
            return new AuthorizationDecision(permitido);
        };
    }
}
