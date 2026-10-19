package com.fitstore.common.feign;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;

/** Configuración base de Resilience4j para las llamadas Feign entre servicios (ADR-010). */
@AutoConfiguration(afterName = "org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JAutoConfiguration")
@ConditionalOnClass(Resilience4JCircuitBreakerFactory.class)
@EnableConfigurationProperties(ResilienciaAutoConfiguration.ResilienciaProperties.class)
public class ResilienciaAutoConfiguration {

    /**
     * Valores base de todos los circuit breakers ({@code fitstore.resilience.*}).
     *
     * @param umbralFallos        porcentaje de fallos que abre el circuito (por defecto 50)
     * @param ventana             llamadas que se miran para calcularlo (10)
     * @param esperaAbiertoMs     tiempo con el circuito abierto antes de probar de nuevo (10000)
     * @param llamadasSemiabierto llamadas de prueba con el circuito semiabierto (3)
     * @param timeoutMs           tiempo máximo de una llamada (10000; los cobros pueden tardar)
     */
    @ConfigurationProperties(prefix = "fitstore.resilience")
    public record ResilienciaProperties(Float umbralFallos, Integer ventana, Long esperaAbiertoMs,
            Integer llamadasSemiabierto, Long timeoutMs) {
        public ResilienciaProperties {
            umbralFallos = umbralFallos == null ? 50f : umbralFallos;
            ventana = ventana == null ? 10 : ventana;
            esperaAbiertoMs = esperaAbiertoMs == null ? 10_000L : esperaAbiertoMs;
            llamadasSemiabierto = llamadasSemiabierto == null ? 3 : llamadasSemiabierto;
            timeoutMs = timeoutMs == null ? 10_000L : timeoutMs;
        }
    }

    @Bean
    Customizer<Resilience4JCircuitBreakerFactory> fitstoreResilienciaBase(ResilienciaProperties p) {
        return fabrica -> fabrica.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(p.umbralFallos())
                        .slidingWindowSize(p.ventana())
                        .waitDurationInOpenState(Duration.ofMillis(p.esperaAbiertoMs()))
                        .permittedNumberOfCallsInHalfOpenState(p.llamadasSemiabierto())
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofMillis(p.timeoutMs()))
                        .build())
                .build());
    }
}
