package com.fitstore.catalog.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Cada evento que publica catalog tiene su routing key, coincide con el AsyncAPI, y los que consume se leen del sobre. */
class CatalogoRutasEventosCatalogTest {

    private static final Path ASYNCAPI = Path.of("../../contracts/asyncapi/fitstore-eventos.yaml");

    private final CatalogoRutasEventosCatalog rutas = new CatalogoRutasEventosCatalog();
    private final JsonMapper json = JsonMapper.builder().build();

    @SuppressWarnings("unchecked")
    private Map<String, Object> asyncApi() throws Exception {
        return new Yaml().load(Files.readString(ASYNCAPI));
    }

    @Test
    @DisplayName("CA-0a-06 cada evento que publica catalog tiene su routing key")
    void routingKeys() {
        assertThat(rutas.routingKey("StockBajo")).isEqualTo("catalog.stock.bajo");
        assertThat(rutas.routingKey("StockNoConfirmado")).isEqualTo("catalog.stock.no-confirmado");
    }

    @Test
    @DisplayName("CA-0a-06 un tipo de evento desconocido o ajeno se rechaza")
    void tipoDesconocido() {
        assertThatThrownBy(() -> rutas.routingKey("PedidoPagado"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PedidoPagado");
    }

    @Test
    @DisplayName("CA-0a-06 las routing keys coinciden con las direcciones de los canales del AsyncAPI")
    @SuppressWarnings("unchecked")
    void coincideConElAsyncApi() throws Exception {
        Map<String, Map<String, Object>> canales = (Map<String, Map<String, Object>>) asyncApi().get("channels");

        for (String tipo : List.of("StockBajo", "StockNoConfirmado")) {
            String canal = Character.toLowerCase(tipo.charAt(0)) + tipo.substring(1);
            assertThat(rutas.routingKey(tipo)).isEqualTo(canales.get(canal).get("address"));
        }
    }

    @Test
    @DisplayName("CA-0a-06 los campos de los eventos que publica coinciden con los requeridos por el AsyncAPI")
    @SuppressWarnings("unchecked")
    void camposCoincidenConElAsyncApi() throws Exception {
        Map<String, Object> componentes = (Map<String, Object>) asyncApi().get("components");
        Map<String, Map<String, Object>> esquemas = (Map<String, Map<String, Object>>) componentes.get("schemas");
        Map<String, Object> eventos = Map.of(
                "SobreStockBajo", new StockBajo(10L, "LEG-01-NEGRO-M", "Legging Pro", "M", "Negro", 4, 5),
                "SobreStockNoConfirmado", new StockNoConfirmado(UUID.randomUUID(),
                        List.of(new StockNoConfirmado.Faltante(10L, "LEG-01-NEGRO-M", 3, 1))));

        for (var e : eventos.entrySet()) {
            List<Map<String, Object>> allOf = (List<Map<String, Object>>) esquemas.get(e.getKey()).get("allOf");
            Map<String, Object> props = (Map<String, Object>) allOf.get(1).get("properties");
            Map<String, Object> datos = (Map<String, Object>) props.get("datos");
            Set<String> requeridos = Set.copyOf((List<String>) datos.get("required"));

            JsonNode nodo = json.readTree(json.writeValueAsString(e.getValue()));
            Set<String> campos = nodo.propertyNames().stream().collect(Collectors.toSet());
            assertThat(campos).as(e.getKey()).containsAll(requeridos);
            assertThat(((Map<String, Object>) datos.get("properties")).keySet()).as(e.getKey()).containsAll(campos);
        }
    }

    @Test
    @DisplayName("CA-0a-06 los eventos que consume se leen del sobre e ignoran los campos desconocidos")
    void leeLosEventosQueConsume() throws Exception {
        UUID pedido = UUID.randomUUID();
        PedidoPagado pagado = json.readValue("""
                {"pedidoId":"%s","codigo":"FS-0001","campoNuevo":true,"items":[{"varianteId":7,"cantidad":2}]}
                """.formatted(pedido), PedidoPagado.class);
        PedidoCancelado cancelado = json.readValue("""
                {"pedidoId":"%s","codigo":"FS-0001","fuePagado":true,"motivo":null,"actor":"STAFF",
                 "items":[{"varianteId":7,"cantidad":2}]}
                """.formatted(pedido), PedidoCancelado.class);
        PedidoExpirado expirado = json.readValue("""
                {"pedidoId":"%s","codigo":"FS-0001"}
                """.formatted(pedido), PedidoExpirado.class);

        assertThat(pagado.items()).containsExactly(new ItemCantidad(7L, 2));
        assertThat(cancelado.fuePagado()).isTrue();
        assertThat(cancelado.motivo()).isNull();
        assertThat(cancelado.actor()).isEqualTo("STAFF");
        assertThat(expirado.pedidoId()).isEqualTo(pedido);
    }
}
