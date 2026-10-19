package com.fitstore.common.feign;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JAutoConfiguration;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

/** Infraestructura 7 (Feign): configuración base de Resilience4j para las llamadas entre servicios (ADR-010). */
class ResilienciaBaseTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withBean(CircuitBreakerRegistry.class, CircuitBreakerRegistry::ofDefaults)
            .withBean(TimeLimiterRegistry.class, TimeLimiterRegistry::ofDefaults)
            .withConfiguration(AutoConfigurations.of(Resilience4JAutoConfiguration.class, ResilienciaAutoConfiguration.class));

    @Test
    @DisplayName("Resilience4j: sin propiedades se aplican los valores base (50 %, ventana 10, 10 s abierto, 3 de prueba, timeout 10 s)")
    void valoresBase() {
        contexto.run(ctx -> {
            ctx.getBean(CircuitBreakerFactory.class).create("catalog-service").run(() -> "ok", t -> "fallback");

            CircuitBreakerConfig config = ctx.getBean(CircuitBreakerRegistry.class)
                    .circuitBreaker("catalog-service").getCircuitBreakerConfig();
            assertThat(config.getFailureRateThreshold()).isEqualTo(50f);
            assertThat(config.getSlidingWindowSize()).isEqualTo(10);
            assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(10_000L);
            assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
            assertThat(ctx.getBean(TimeLimiterRegistry.class).timeLimiter("catalog-service")
                    .getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofSeconds(10));
        });
    }

    @Test
    @DisplayName("Resilience4j: los valores se cambian con propiedades fitstore.resilience.*")
    void valoresPorPropiedades() {
        contexto.withPropertyValues(
                        "fitstore.resilience.umbral-fallos=30",
                        "fitstore.resilience.ventana=5",
                        "fitstore.resilience.espera-abierto-ms=2000",
                        "fitstore.resilience.llamadas-semiabierto=2",
                        "fitstore.resilience.timeout-ms=1500")
                .run(ctx -> {
                    ctx.getBean(CircuitBreakerFactory.class).create("order-service").run(() -> "ok", t -> "fallback");

                    CircuitBreakerConfig config = ctx.getBean(CircuitBreakerRegistry.class)
                            .circuitBreaker("order-service").getCircuitBreakerConfig();
                    assertThat(config.getFailureRateThreshold()).isEqualTo(30f);
                    assertThat(config.getSlidingWindowSize()).isEqualTo(5);
                    assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(2_000L);
                    assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(2);
                    assertThat(ctx.getBean(TimeLimiterRegistry.class).timeLimiter("order-service")
                            .getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofMillis(1500));
                });
    }
}
