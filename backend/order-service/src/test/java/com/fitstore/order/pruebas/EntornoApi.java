package com.fitstore.order.pruebas;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Entorno de las pruebas de API: lo de {@link EntornoPrueba} (PostgreSQL y Redis) más un WireMock que hace de catalog,
 * auth, delivery y settings (sus rutas no se solapan, así que un solo servidor basta). Los clientes Feign apuntan a él.
 */
public class EntornoApi implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /** Servidor simulado compartido; {@code ApiBase} lo reinicia antes de cada prueba. */
    public static final WireMockServer SERVICIOS = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        SERVICIOS.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext contexto) {
        new EntornoPrueba().initialize(contexto);
        String url = "http://localhost:" + SERVICIOS.port();
        TestPropertyValues.of(
                "fitstore.clientes.catalog.url=" + url,
                "fitstore.clientes.auth.url=" + url,
                "fitstore.clientes.delivery.url=" + url,
                "fitstore.clientes.settings.url=" + url).applyTo(contexto);
    }
}
