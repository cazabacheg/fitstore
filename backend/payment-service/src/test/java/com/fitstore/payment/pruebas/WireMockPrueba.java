package com.fitstore.payment.pruebas;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;

/** Mercado Pago simulado con WireMock (no hay credenciales reales): un servidor compartido por las pruebas de payment. */
public final class WireMockPrueba {

    private static final WireMockServer SERVIDOR = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        SERVIDOR.start();
    }

    private WireMockPrueba() {
    }

    public static WireMockServer servidor() {
        return SERVIDOR;
    }

    public static String url() {
        return "http://localhost:" + SERVIDOR.port();
    }

    /** Apunta el cliente de Mercado Pago al WireMock. */
    public static void registrar(DynamicPropertyRegistry registro) {
        registro.add("fitstore.pagos.mercadopago.url", WireMockPrueba::url);
    }
}
