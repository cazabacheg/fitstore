package com.fitstore.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fitstore.auth.config.PropiedadesAuth;
import com.fitstore.auth.model.TipoCuenta;
import com.fitstore.auth.pruebas.PruebaApi;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Contador de fallos y bloqueo con Redis real en Testcontainers (CA-1-06 y CA-1-24). */
class RegistroIntentosLoginTest extends PruebaApi {

    @Autowired RegistroIntentosLogin intentos;
    @Autowired PropiedadesAuth propiedades;

    @Test
    @DisplayName("CA-1-06 el quinto fallo seguido bloquea el correo y el contador y el bloqueo duran 15 minutos")
    void quintoFalloBloquea() {
        for (int i = 0; i < 4; i++) {
            intentos.registrarFallo(TipoCuenta.CLIENTE, "ana@correo.com");
        }
        assertThat(intentos.estaBloqueado(TipoCuenta.CLIENTE, "ana@correo.com")).isFalse();
        assertThat(redis.getExpire("auth:login:fallos:cliente:ana@correo.com")).isBetween(1L, 900L);

        intentos.registrarFallo(TipoCuenta.CLIENTE, "ana@correo.com");

        assertThat(intentos.estaBloqueado(TipoCuenta.CLIENTE, "ana@correo.com")).isTrue();
        assertThat(redis.getExpire("auth:login:bloqueo:cliente:ana@correo.com")).isBetween(1L, 900L);
    }

    @Test
    @DisplayName("CA-1-24 el contador es independiente por tipo de cuenta y por correo")
    void independientePorTipoYCorreo() {
        for (int i = 0; i < 5; i++) {
            intentos.registrarFallo(TipoCuenta.CLIENTE, "mismo@correo.com");
        }

        assertThat(intentos.estaBloqueado(TipoCuenta.CLIENTE, "mismo@correo.com")).isTrue();
        assertThat(intentos.estaBloqueado(TipoCuenta.STAFF, "mismo@correo.com")).isFalse();
        assertThat(intentos.estaBloqueado(TipoCuenta.CLIENTE, "otro@correo.com")).isFalse();
    }

    @Test
    @DisplayName("CA-1-06 limpiar borra el contador de fallos")
    void limpiarBorraElContador() {
        for (int i = 0; i < 3; i++) {
            intentos.registrarFallo(TipoCuenta.CLIENTE, "ana@correo.com");
        }

        intentos.limpiar(TipoCuenta.CLIENTE, "ana@correo.com");

        assertThat(redis.hasKey("auth:login:fallos:cliente:ana@correo.com")).isFalse();
        for (int i = 0; i < 4; i++) {
            intentos.registrarFallo(TipoCuenta.CLIENTE, "ana@correo.com");
        }
        assertThat(intentos.estaBloqueado(TipoCuenta.CLIENTE, "ana@correo.com")).isFalse();
    }

    @Test
    @DisplayName("CA-1-05 si Redis no responde, el login sigue funcionando sin bloqueo (micro 3.2)")
    void sinRedisNoBloquea() {
        LettuceClientConfiguration cliente = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(300))
                .clientOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofMillis(300)).build()).build())
                .build();
        LettuceConnectionFactory fabrica = new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 1), cliente);
        fabrica.afterPropertiesSet();
        RegistroIntentosLogin caido = new RegistroIntentosLogin(new StringRedisTemplate(fabrica), propiedades);
        try {
            assertThatCode(() -> caido.registrarFallo(TipoCuenta.CLIENTE, "ana@correo.com")).doesNotThrowAnyException();
            assertThatCode(() -> caido.limpiar(TipoCuenta.CLIENTE, "ana@correo.com")).doesNotThrowAnyException();
            assertThat(caido.estaBloqueado(TipoCuenta.CLIENTE, "ana@correo.com")).isFalse();
        } finally {
            fabrica.destroy();
        }
    }
}
