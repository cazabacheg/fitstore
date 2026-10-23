package com.fitstore.order.carrito;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.order.carrito.model.CarritoId;
import com.fitstore.order.carrito.repository.CarritoRedisRepository;
import com.fitstore.order.carrito.repository.CarritoRepository;
import com.fitstore.order.config.PropiedadesOrder;
import com.fitstore.order.pruebas.EntornoPrueba;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/** CA-0a-06: capa de datos del carrito, con Redis en Testcontainers (micro 3.2: hash por carrito, TTL, fusión). */
@SpringBootTest(classes = CarritoRepositoryTest.Contexto.class, properties = {
    "fitstore.order.carrito.max-lineas=50",
    "fitstore.order.carrito.max-unidades-linea=100",
    "fitstore.order.carrito.ttl-anonimo=7d",
    "fitstore.order.carrito.ttl-cliente=30d",
    "fitstore.order.codigo-prefijo=FS",
    "fitstore.order.settings-cache-ttl=5m",
    "fitstore.order.expirador-cron=0 * * * * *",
    "fitstore.order.defaults.expiracion-pedido-minutos=30"
})
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = EntornoPrueba.class)
class CarritoRepositoryTest {

    @Configuration
    @ImportAutoConfiguration(DataRedisAutoConfiguration.class)
    @EnableConfigurationProperties(PropiedadesOrder.class)
    @Import(CarritoRedisRepository.class)
    static class Contexto {
    }

    @Autowired CarritoRepository repositorio;
    @Autowired StringRedisTemplate redis;

    private final CarritoId cliente = CarritoId.cliente(UUID.randomUUID());
    private final CarritoId anonimo = CarritoId.anonimo(UUID.randomUUID().toString());

    @BeforeEach
    void limpiar() {
        redis.execute((org.springframework.data.redis.core.RedisCallback<Object>) c -> {
            c.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    @DisplayName("CA-0a-06 agrega líneas al carrito y las lista")
    void agregaYLista() {
        repositorio.establecerLinea(cliente, 10L, 2);
        repositorio.establecerLinea(cliente, 11L, 1);

        assertThat(repositorio.obtenerLineas(cliente)).containsExactlyInAnyOrderEntriesOf(Map.of(10L, 2, 11L, 1));
        assertThat(repositorio.obtenerLineas(anonimo)).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 usa las claves order:carrito:cli y order:carrito:anon")
    void clavesConPrefijo() {
        repositorio.establecerLinea(cliente, 10L, 1);
        repositorio.establecerLinea(anonimo, 10L, 1);

        assertThat(redis.hasKey("order:carrito:cli:" + cliente.valor())).isTrue();
        assertThat(redis.hasKey("order:carrito:anon:" + anonimo.valor())).isTrue();
    }

    @Test
    @DisplayName("CA-0a-06 cambia la cantidad de una línea sin duplicarla")
    void actualizaCantidad() {
        repositorio.establecerLinea(cliente, 10L, 2);
        repositorio.establecerLinea(cliente, 10L, 5);

        assertThat(repositorio.obtenerLineas(cliente)).containsExactlyEntriesOf(Map.of(10L, 5));
    }

    @Test
    @DisplayName("CA-0a-06 quita una línea y, al quitar la última, desaparece el carrito con su cotización")
    void quitaLinea() {
        repositorio.establecerLinea(cliente, 10L, 2);
        repositorio.establecerLinea(cliente, 11L, 1);
        repositorio.guardarCotizacion(cliente, "{\"total\":100.00}");

        repositorio.quitarLinea(cliente, 10L);
        assertThat(repositorio.obtenerLineas(cliente)).containsExactlyEntriesOf(Map.of(11L, 1));
        assertThat(repositorio.leerCotizacion(cliente)).isPresent();

        repositorio.quitarLinea(cliente, 11L);
        assertThat(repositorio.obtenerLineas(cliente)).isEmpty();
        assertThat(repositorio.leerCotizacion(cliente)).isEmpty();
        assertThat(repositorio.ttl(cliente)).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 vacía el carrito completo")
    void vacia() {
        repositorio.establecerLinea(cliente, 10L, 2);
        repositorio.guardarCotizacion(cliente, "{}");

        repositorio.vaciar(cliente);

        assertThat(repositorio.obtenerLineas(cliente)).isEmpty();
        assertThat(repositorio.leerCotizacion(cliente)).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 y CA-4-11 el carrito del cliente dura 30 días y el anónimo 7")
    void ttlPorTipo() {
        repositorio.establecerLinea(cliente, 10L, 1);
        repositorio.establecerLinea(anonimo, 10L, 1);

        assertThat(repositorio.ttl(cliente).orElseThrow()).isBetween(Duration.ofDays(30).minusMinutes(1), Duration.ofDays(30));
        assertThat(repositorio.ttl(anonimo).orElseThrow()).isBetween(Duration.ofDays(7).minusMinutes(1), Duration.ofDays(7));
    }

    @Test
    @DisplayName("CA-0a-06 cada cambio renueva el TTL del carrito y de su cotización")
    void renuevaTtl() {
        repositorio.establecerLinea(cliente, 10L, 1);
        repositorio.guardarCotizacion(cliente, "{}");
        redis.expire("order:carrito:cli:" + cliente.valor(), Duration.ofMinutes(5));
        redis.expire("order:carrito:cli:" + cliente.valor() + ":cotizacion", Duration.ofMinutes(5));

        repositorio.establecerLinea(cliente, 10L, 2);

        assertThat(repositorio.ttl(cliente).orElseThrow()).isGreaterThan(Duration.ofDays(29));
        Long ttlCotizacion = redis.getExpire("order:carrito:cli:" + cliente.valor() + ":cotizacion");
        assertThat(ttlCotizacion).isGreaterThan(Duration.ofDays(29).toSeconds());
    }

    @Test
    @DisplayName("CA-0a-06 guarda y lee la última cotización como JSON, con el mismo TTL del carrito")
    void cotizacion() {
        repositorio.establecerLinea(anonimo, 10L, 1);

        repositorio.guardarCotizacion(anonimo, "{\"total\":50.00}");

        assertThat(repositorio.leerCotizacion(anonimo)).contains("{\"total\":50.00}");
        Long ttl = redis.getExpire("order:carrito:anon:" + anonimo.valor() + ":cotizacion");
        assertThat(ttl).isBetween(Duration.ofDays(7).minusMinutes(1).toSeconds(), Duration.ofDays(7).toSeconds());
        assertThat(repositorio.leerCotizacion(cliente)).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 fusiona el carrito anónimo con el del cliente sumando cantidades")
    void fusiona() {
        repositorio.establecerLinea(anonimo, 10L, 2);
        repositorio.establecerLinea(anonimo, 12L, 1);
        repositorio.establecerLinea(cliente, 10L, 3);
        repositorio.guardarCotizacion(cliente, "{\"vieja\":true}");

        Map<Long, Integer> resultado = repositorio.fusionar(anonimo, cliente);

        assertThat(resultado).containsExactlyInAnyOrderEntriesOf(Map.of(10L, 5, 12L, 1));
        assertThat(repositorio.obtenerLineas(cliente)).isEqualTo(resultado);
        assertThat(repositorio.obtenerLineas(anonimo)).isEmpty();
        assertThat(repositorio.leerCotizacion(cliente)).isEmpty();
        assertThat(repositorio.ttl(cliente).orElseThrow()).isGreaterThan(Duration.ofDays(29));
    }

    @Test
    @DisplayName("CA-0a-06 si el cliente no tenía carrito, el anónimo pasa a ser suyo")
    void fusionSinCarritoDelCliente() {
        repositorio.establecerLinea(anonimo, 10L, 2);

        Map<Long, Integer> resultado = repositorio.fusionar(anonimo, cliente);

        assertThat(resultado).containsExactlyEntriesOf(Map.of(10L, 2));
        assertThat(repositorio.obtenerLineas(anonimo)).isEmpty();
        assertThat(repositorio.ttl(cliente).orElseThrow()).isGreaterThan(Duration.ofDays(29));
    }

    @Test
    @DisplayName("CA-0a-06 fusionar un carrito anónimo vacío no cambia el del cliente")
    void fusionDeAnonimoVacio() {
        repositorio.establecerLinea(cliente, 10L, 3);

        Map<Long, Integer> resultado = repositorio.fusionar(anonimo, cliente);

        assertThat(resultado).containsExactlyEntriesOf(Map.of(10L, 3));
    }
}
