package com.fitstore.common.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seguridad base de los servicios de negocio (SPEC-0a I3): <em>resource server</em> JWT, todo cerrado salvo
 * {@code /actuator/health} y la documentación OpenAPI; cada spec abre sus endpoints con {@code @PreAuthorize}.
 * Jerarquía de roles {@code ADMIN > OPERADOR}; el claim {@code rol} se convierte en la autoridad {@code ROLE_*}.
 */
@AutoConfiguration(beforeName = {
    "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
    "org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration"
})
@ConditionalOnClass({SecurityFilterChain.class, JwtDecoder.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBooleanProperty(name = "fitstore.security.habilitado", matchIfMissing = true)
@EnableConfigurationProperties(FitstoreSecurityProperties.class)
@EnableWebSecurity
@EnableMethodSecurity
public class FitstoreSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_OPERADOR");
    }

    /** Dev: valida con la clave pública de un archivo JWKS (SPEC-0a I2); sin la propiedad se usa {@code jwk-set-uri}. */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    @ConditionalOnProperty(prefix = "fitstore.security", name = "jwks-archivo")
    JwtDecoder jwtDecoderDeArchivo(FitstoreSecurityProperties propiedades) {
        try {
            String contenido = Files.readString(Path.of(propiedades.jwksArchivo()), StandardCharsets.UTF_8);
            List<JWK> claves = JWKSet.parse(contenido).getKeys();
            RSAKey rsa = claves.stream()
                    .filter(RSAKey.class::isInstance)
                    .map(RSAKey.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("El JWKS de " + propiedades.jwksArchivo() + " no tiene clave RSA"));
            return NimbusJwtDecoder.withPublicKey(rsa.toRSAPublicKey()).build();
        } catch (IOException | ParseException | com.nimbusds.jose.JOSEException e) {
            throw new IllegalStateException("No se pudo leer el JWKS " + propiedades.jwksArchivo(), e);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    JwtAuthenticationConverter fitstoreJwtAuthenticationConverter() {
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(jwt -> {
            String rol = jwt.getClaimAsString("rol");
            return rol == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        });
        return convertidor;
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain fitstoreSecurityFilterChain(HttpSecurity http, JsonMapper json,
            JwtAuthenticationConverter convertidor) throws Exception {
        RespuestasSeguridad respuestas = new RespuestasSeguridad(json);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**",
                                "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter((org.springframework.core.convert.converter.Converter<
                                org.springframework.security.oauth2.jwt.Jwt, ? extends AbstractAuthenticationToken>) convertidor))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .exceptionHandling(e -> e.authenticationEntryPoint(respuestas).accessDeniedHandler(respuestas))
                .build();
    }
}
