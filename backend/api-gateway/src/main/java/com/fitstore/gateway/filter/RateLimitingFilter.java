package com.fitstore.gateway.filter;

import com.fitstore.gateway.config.GatewayProperties;
import com.fitstore.gateway.config.GatewayProperties.Clave;
import com.fitstore.gateway.config.GatewayProperties.Regla;
import com.fitstore.gateway.web.RespuestasError;
import java.net.InetSocketAddress;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * Rate limiting con Redis (ADR-020, infraestructura 4.4): contador por ventana fija con clave de IP o de usuario.
 * Al superar el límite responde 429 con {@code Retry-After}. Si Redis no está disponible, deja pasar la petición y
 * registra una advertencia: la tienda no se bloquea.
 */
@Component
public class RateLimitingFilter implements WebFilter, Ordered {

    /** Después de la seguridad (-100) y del bloqueo de rutas internas (-90). */
    public static final int ORDEN = -80;

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitingFilter.class);

    /** Incrementa el contador; la primera vez fija la expiración de la ventana. Devuelve {contador, segundos restantes}. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final DefaultRedisScript<List> CONTADOR = new DefaultRedisScript<>("""
            local c = redis.call('INCR', KEYS[1])
            if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return {c, redis.call('TTL', KEYS[1])}
            """, List.class);

    private record ReglaCompilada(Regla regla, List<PathPattern> patrones) {
    }

    private record Veredicto(boolean permitido, long reintentarEnSegundos) {
        static final Veredicto PERMITIDO = new Veredicto(true, 0);
    }

    private final ReactiveStringRedisTemplate redis;
    private final JsonMapper json;
    private final List<ReglaCompilada> reglas;
    private final Regla resto;

    public RateLimitingFilter(ReactiveStringRedisTemplate redis, JsonMapper json, GatewayProperties propiedades) {
        this.redis = redis;
        this.json = json;
        PathPatternParser parser = PathPatternParser.defaultInstance;
        this.reglas = propiedades.rateLimit().reglas().stream()
                .map(r -> new ReglaCompilada(r, r.rutas().stream().map(parser::parse).toList()))
                .toList();
        var r = propiedades.rateLimit().resto();
        this.resto = new Regla("resto", null, List.of(), r.limite(), r.ventanaSegundos(), Clave.IP);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange intercambio, WebFilterChain cadena) {
        if (intercambio.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return cadena.filter(intercambio); // el preflight de CORS no cuenta
        }
        Regla regla = elegirRegla(intercambio.getRequest());
        return claveDe(regla, intercambio)
                .flatMap(clave -> consultar(regla, clave))
                .onErrorResume(error -> {
                    LOG.warn("Redis no está disponible; se omite el rate limiting y la petición pasa: {}", error.toString());
                    return Mono.just(Veredicto.PERMITIDO);
                })
                .flatMap(veredicto -> veredicto.permitido()
                        ? cadena.filter(intercambio)
                        : rechazar(intercambio, veredicto.reintentarEnSegundos()));
    }

    private Regla elegirRegla(ServerHttpRequest peticion) {
        PathContainer ruta = peticion.getPath().pathWithinApplication();
        for (ReglaCompilada compilada : reglas) {
            boolean metodo = compilada.regla().metodo() == null
                    || compilada.regla().metodo().equalsIgnoreCase(peticion.getMethod().name());
            if (metodo && compilada.patrones().stream().anyMatch(p -> p.matches(ruta))) {
                return compilada.regla();
            }
        }
        return resto;
    }

    private Mono<String> claveDe(Regla regla, ServerWebExchange intercambio) {
        Mono<String> ip = Mono.fromSupplier(() -> ipDe(intercambio.getRequest()));
        if (regla.clave() == Clave.USUARIO) {
            return ReactiveSecurityContextHolder.getContext()
                    .map(SecurityContext::getAuthentication)
                    .filter(Authentication::isAuthenticated)
                    .map(Authentication::getName)
                    .map(sub -> "u:" + sub)
                    .switchIfEmpty(ip.map(valor -> "ip:" + valor));
        }
        return ip.map(valor -> "ip:" + valor);
    }

    /** La IP real viene de {@code X-Forwarded-For} puesto por Caddy (solo se confía en Caddy). */
    private static String ipDe(ServerHttpRequest peticion) {
        String reenviada = peticion.getHeaders().getFirst("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        InetSocketAddress remota = peticion.getRemoteAddress();
        return remota != null && remota.getAddress() != null ? remota.getAddress().getHostAddress() : "desconocida";
    }

    @SuppressWarnings("unchecked")
    private Mono<Veredicto> consultar(Regla regla, String clave) {
        String llave = "rl:" + regla.nombre() + ":" + clave;
        return redis.execute(CONTADOR, List.of(llave), List.of(String.valueOf(regla.ventanaSegundos())))
                .next()
                .map(resultado -> {
                    List<Long> valores = (List<Long>) resultado;
                    long contador = valores.get(0);
                    long restantes = Math.max(1, valores.get(1));
                    return contador <= regla.limite() ? Veredicto.PERMITIDO : new Veredicto(false, restantes);
                });
    }

    private Mono<Void> rechazar(ServerWebExchange intercambio, long reintentarEn) {
        intercambio.getResponse().getHeaders().set("Retry-After", String.valueOf(reintentarEn));
        return RespuestasError.escribir(intercambio, json, HttpStatus.TOO_MANY_REQUESTS,
                RespuestasError.DEMASIADOS_INTENTOS, "DEMASIADOS_INTENTOS");
    }

    @Override
    public int getOrder() {
        return ORDEN;
    }
}
