package com.fitstore.order.pruebas;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Respuestas de catalog, auth, delivery y settings en WireMock, con la forma de sus contratos
 * ({@code contracts/openapi}). Las pruebas de order las usan para no depender de los otros servicios.
 */
public final class Simulados {

    private Simulados() {
    }

    /** Una línea de la cotización de catalog (LineaCotizada). */
    public record Linea(long varianteId, long productoId, String sku, String producto, String talla, String color,
            String precio, int cantidad, String descuentoPorcentaje, String estado, Integer disponible) {

        public static Linea ok(long varianteId, String producto, String precio, int cantidad) {
            return new Linea(varianteId, varianteId / 10, "SKU-" + varianteId, producto, "M", "Negro", precio,
                    cantidad, "0", "OK", null);
        }

        public Linea conDescuento(String porcentaje) {
            return new Linea(varianteId, productoId, sku, producto, talla, color, precio, cantidad, porcentaje,
                    estado, disponible);
        }

        public Linea conEstado(String nuevoEstado, Integer cuantosQuedan) {
            return new Linea(varianteId, productoId, sku, producto, talla, color, precio, cantidad,
                    descuentoPorcentaje, nuevoEstado, cuantosQuedan);
        }

        BigDecimal subtotal() {
            return "NO_DISPONIBLE".equals(estado) ? BigDecimal.ZERO : new BigDecimal(precio).multiply(BigDecimal.valueOf(cantidad));
        }

        BigDecimal descuento() {
            return subtotal().multiply(new BigDecimal(descuentoPorcentaje)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
    }

    // ---------- catalog ----------

    public static String cotizacion(Linea... lineas) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuento = BigDecimal.ZERO;
        String filas = java.util.Arrays.stream(lineas).map(l -> {
            return """
                {"varianteId":%d,"productoId":%d,"sku":"%s","producto":"%s","slug":"slug-%d","talla":"%s","color":"%s",
                 "imagenUrl":"http://media/%d.jpg","precioUnitario":%s,"cantidad":%d,"subtotal":%s,"descuentoPorcentaje":%s,
                 "descuentoMonto":%s,"total":%s,"estado":"%s","disponible":%s}"""
                    .formatted(l.varianteId(), l.productoId(), l.sku(), l.producto(), l.productoId(), l.talla(),
                            l.color(), l.varianteId(), l.precio(), l.cantidad(), l.subtotal().toPlainString(),
                            l.descuentoPorcentaje(), l.descuento().toPlainString(),
                            l.subtotal().subtract(l.descuento()).toPlainString(), l.estado(),
                            l.disponible() == null ? "null" : l.disponible().toString());
        }).collect(Collectors.joining(","));
        for (Linea l : lineas) {
            subtotal = subtotal.add(l.subtotal());
            descuento = descuento.add(l.descuento());
        }
        String productos = java.util.Arrays.stream(lineas)
                .map(l -> "{\"productoId\":%d,\"nombre\":\"%s\",\"unidades\":%d,\"tramoAlcanzado\":null,\"siguienteTramo\":{\"unidadesMinimas\":%d,\"porcentaje\":15,\"faltan\":2}}"
                        .formatted(l.productoId(), l.producto(), l.cantidad(), l.cantidad() + 2))
                .collect(Collectors.joining(","));
        return """
                {"success":true,"mensaje":"Operación exitosa","data":{"lineas":[%s],"productos":[%s],
                 "subtotal":%s,"descuentoTotal":%s,"total":%s}}"""
                .formatted(filas, productos, subtotal.toPlainString(), descuento.toPlainString(),
                        subtotal.subtract(descuento).toPlainString());
    }

    public static void cotizar(Linea... lineas) {
        cotizarJson(cotizacion(lineas));
    }

    public static void cotizarJson(String json) {
        EntornoApi.SERVICIOS.stubFor(post(urlEqualTo("/api/v1/precios/cotizar"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(json)));
    }

    public static void catalogCaido() {
        EntornoApi.SERVICIOS.stubFor(post(urlEqualTo("/api/v1/precios/cotizar"))
                .willReturn(aResponse().withStatus(503).withHeader("Content-Type", "application/json")
                        .withBody("{\"success\":false,\"mensaje\":\"caído\",\"codigo\":\"SERVICIO_NO_DISPONIBLE\"}")));
    }

    public static void catalogSinRespuesta() {
        EntornoApi.SERVICIOS.stubFor(post(urlEqualTo("/api/v1/precios/cotizar"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    public static void reservaCreada() {
        EntornoApi.SERVICIOS.stubFor(post(urlEqualTo("/internal/v1/inventario/reservas"))
                .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"success":true,"mensaje":"Operación exitosa","data":{"pedidoId":"%s","estado":"ACTIVA",
                                 "expiraEn":"2030-01-01T00:00:00Z","items":[]}}""".formatted(new java.util.UUID(0, 0)))));
    }

    public static void reservaSinStock(long varianteId, int solicitado, int disponible) {
        EntornoApi.SERVICIOS.stubFor(post(urlEqualTo("/internal/v1/inventario/reservas"))
                .willReturn(aResponse().withStatus(409).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"success":false,"mensaje":"Sin stock para algunos productos","codigo":"SIN_STOCK",
                                 "detalles":{"faltantes":[{"varianteId":%d,"solicitado":%d,"disponible":%d}]}}"""
                                .formatted(varianteId, solicitado, disponible))));
    }

    public static void reservaCaida() {
        EntornoApi.SERVICIOS.stubFor(post(urlEqualTo("/internal/v1/inventario/reservas"))
                .willReturn(aResponse().withStatus(503)));
    }

    // ---------- auth ----------

    public static void perfil(String id) {
        EntornoApi.SERVICIOS.stubFor(get(urlEqualTo("/api/v1/clientes/me"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("""
                        {"success":true,"mensaje":"ok","data":{"id":"%s","email":"ana@correo.pe","nombres":"Ana",
                         "apellidos":"Quispe","dni":"12345678","telefono":"987654321","creadoEn":"2026-01-01T00:00:00Z"}}"""
                        .formatted(id))));
    }

    public static void direccion(long id, String ubigeo) {
        EntornoApi.SERVICIOS.stubFor(get(urlEqualTo("/api/v1/clientes/me/direcciones/" + id))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("""
                        {"success":true,"mensaje":"ok","data":{"id":%d,"alias":"Casa","ubigeo":"%s",
                         "direccion":"Jr. Gamarra 123","referencia":"Frente al mercado","telefonoContacto":null,
                         "predeterminada":true}}""".formatted(id, ubigeo))));
    }

    public static void direccionAjena(long id) {
        EntornoApi.SERVICIOS.stubFor(get(urlEqualTo("/api/v1/clientes/me/direcciones/" + id))
                .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json")
                        .withBody("{\"success\":false,\"mensaje\":\"No encontrada\",\"codigo\":\"NO_ENCONTRADO\"}")));
    }

    public static void authCaido() {
        EntornoApi.SERVICIOS.stubFor(get(urlEqualTo("/api/v1/clientes/me")).willReturn(aResponse().withStatus(503)));
    }

    // ---------- settings ----------

    public static void configuracion(List<String> modalidades, int expiracionMinutos) {
        String lista = modalidades.stream().map(m -> "\"" + m + "\"").collect(Collectors.joining(","));
        EntornoApi.SERVICIOS.stubFor(get(urlEqualTo("/api/v1/settings/public"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("""
                        {"success":true,"mensaje":"ok","data":{"identidad":{"nombre":"FitStore"},
                         "contenido":{"horario":"Lun–Sáb 9:00–19:00","tiendaFisica":{"direccion":"Jr. Gamarra 500",
                          "ubigeo":"150115","ubigeoNombre":"La Victoria, Lima, Lima","referencia":"Galería Central"}},
                         "parametros":{"metodosPago":["TARJETA"],"modalidadesEntrega":[%s],
                          "expiracionPedidoMinutos":%d,"diasReembolsoPostventa":30,"moneda":"PEN"}}}"""
                        .formatted(lista, expiracionMinutos))));
    }

    public static void settingsCaido() {
        EntornoApi.SERVICIOS.stubFor(get(urlEqualTo("/api/v1/settings/public")).willReturn(aResponse().withStatus(503)));
    }

    public static void ubigeo(String codigo, String departamento, String provincia, String distrito) {
        EntornoApi.SERVICIOS.stubFor(get(urlEqualTo("/api/v1/ubigeo/" + codigo))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("""
                        {"success":true,"mensaje":"ok","data":{"codigo":"%s","nombre":"%s","departamento":"%s",
                         "provincia":"%s","distrito":"%s","nombreCompleto":"%s, %s, %s"}}"""
                        .formatted(codigo, distrito, departamento, provincia, distrito, distrito, provincia,
                                departamento))));
    }

    // ---------- delivery ----------

    public static void cotizarEnvio(String costo, int min, int max, String agencia) {
        MappingBuilder mapeo = post(urlPathEqualTo("/api/v1/envios/cotizar"));
        EntornoApi.SERVICIOS.stubFor(mapeo.willReturn(aResponse().withHeader("Content-Type", "application/json")
                .withBody("""
                        {"success":true,"mensaje":"ok","data":{"costo":%s,"plazoDiasMin":%d,"plazoDiasMax":%d,
                         "agenciaNombre":%s,"nivelTarifa":"DISTRITO","ubigeoTarifa":"150101"}}"""
                        .formatted(costo, min, max, agencia == null ? "null" : "\"" + agencia + "\""))));
    }

    public static void envioSinCobertura() {
        EntornoApi.SERVICIOS.stubFor(post(urlPathEqualTo("/api/v1/envios/cotizar"))
                .willReturn(aResponse().withStatus(422).withHeader("Content-Type", "application/json")
                        .withBody("{\"success\":false,\"mensaje\":\"Sin cobertura\",\"codigo\":\"SIN_COBERTURA\"}")));
    }

    public static void deliveryCaido() {
        EntornoApi.SERVICIOS.stubFor(post(urlPathEqualTo("/api/v1/envios/cotizar")).willReturn(aResponse().withStatus(503)));
    }
}
