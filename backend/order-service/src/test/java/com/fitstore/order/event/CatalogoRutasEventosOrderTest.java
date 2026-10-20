package com.fitstore.order.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.MotivoReembolso;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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

/** Cada evento que publica order tiene su routing key, y sus clases coinciden con el canal y el payload del AsyncAPI. */
class CatalogoRutasEventosOrderTest {

    private static final Path ASYNCAPI = Path.of("../../contracts/asyncapi/fitstore-eventos.yaml");

    private static final Map<String, String> PUBLICADOS = Map.of(
            "PedidoCreado", "order.pedido.creado",
            "PedidoPagado", "order.pedido.pagado",
            "PedidoCancelado", "order.pedido.cancelado",
            "PedidoExpirado", "order.pedido.expirado",
            "PagoNoAplicable", "order.pago.no-aplicable",
            "PedidoListoDespacho", "order.pedido.listo-despacho",
            "PedidoListoRecojo", "order.pedido.listo-recojo",
            "PedidoRecogido", "order.pedido.recogido",
            "ReembolsoSolicitado", "order.reembolso.solicitado");

    private final CatalogoRutasEventosOrder rutas = new CatalogoRutasEventosOrder();
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    @DisplayName("CA-0a-06 cada uno de los 9 eventos que publica order tiene su routing key")
    void routingKeys() {
        assertThat(PUBLICADOS).hasSize(9);
        PUBLICADOS.forEach((tipo, clave) -> assertThat(rutas.routingKey(tipo)).as(tipo).isEqualTo(clave));
    }

    @Test
    @DisplayName("CA-0a-06 un tipo de evento desconocido se rechaza")
    void tipoDesconocido() {
        assertThatThrownBy(() -> rutas.routingKey("ClienteRegistrado"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ClienteRegistrado");
    }

    @Test
    @DisplayName("CA-0a-06 las routing keys coinciden con las direcciones de los canales del AsyncAPI")
    @SuppressWarnings("unchecked")
    void coincideConElAsyncApi() throws Exception {
        Map<String, Object> raiz = new Yaml().load(Files.readString(ASYNCAPI));
        Map<String, Map<String, Object>> canales = (Map<String, Map<String, Object>>) raiz.get("channels");

        for (String tipo : PUBLICADOS.keySet()) {
            String canal = Character.toLowerCase(tipo.charAt(0)) + tipo.substring(1);
            assertThat(rutas.routingKey(tipo)).as(tipo).isEqualTo(canales.get(canal).get("address"));
        }
    }

    @Test
    @DisplayName("CA-0a-06 los campos de cada evento que order publica o consume cubren los requeridos del AsyncAPI y no inventan otros")
    @SuppressWarnings("unchecked")
    void camposCoincidenConElAsyncApi() throws Exception {
        Map<String, Object> raiz = new Yaml().load(Files.readString(ASYNCAPI));
        Map<String, Map<String, Object>> esquemas =
                (Map<String, Map<String, Object>>) ((Map<String, Object>) raiz.get("components")).get("schemas");

        for (var e : ejemplos().entrySet()) {
            List<Map<String, Object>> allOf = (List<Map<String, Object>>) esquemas.get(e.getKey()).get("allOf");
            Map<String, Object> props = (Map<String, Object>) allOf.get(1).get("properties");
            Map<String, Object> datos = (Map<String, Object>) props.get("datos");
            Set<String> requeridos = Set.copyOf((List<String>) datos.get("required"));
            Set<String> permitidos = Set.copyOf(((Map<String, Object>) datos.get("properties")).keySet());

            JsonNode nodo = json.readTree(json.writeValueAsString(e.getValue()));
            Set<String> campos = nodo.propertyNames().stream().collect(Collectors.toSet());
            assertThat(campos).as(e.getKey()).containsAll(requeridos);
            assertThat(permitidos).as(e.getKey()).containsAll(campos);
        }
    }

    private static Map<String, Object> ejemplos() {
        UUID id = UUID.randomUUID();
        Instant ahora = Instant.now();
        BigDecimal monto = new BigDecimal("10.00");
        var destinatario = new DestinatarioEvento("Ana", "Quispe", "12345678", "987654321", "ana@correo.pe");
        var direccion = new DireccionEvento("150101", "Lima", "Lima", "Lima", "Jr. Gamarra 123", null);
        var persona = new PersonaEvento("Ana Quispe", "ana@correo.pe");
        var item = new ItemPedidoEvento(10L, 1L, "POL-NEG-M", "Polo", "M", "Negro", null, monto, 2,
                BigDecimal.ZERO, BigDecimal.ZERO, monto);
        var cantidad = new ItemCantidadEvento(10L, 2);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("SobrePedidoCreado", new PedidoCreado(id, "FS-000001", id, destinatario, Modalidad.DELIVERY_LOCAL,
                direccion, List.of(item), monto, monto, monto, monto, ahora));
        m.put("SobrePedidoPagado", new PedidoPagado(id, "FS-000001", List.of(cantidad)));
        m.put("SobrePedidoCancelado", new PedidoCancelado(id, "FS-000001", true, "Ya no lo quiero", "CLIENTE",
                List.of(cantidad)));
        m.put("SobrePedidoExpirado", new PedidoExpirado(id, "FS-000001"));
        m.put("SobrePagoNoAplicable", new PagoNoAplicable(id, id, "PEDIDO_EXPIRADO"));
        m.put("SobrePedidoListoDespacho", new PedidoListoDespacho(id, "FS-000001", Modalidad.AGENCIA, destinatario,
                direccion, 5L, "Shalom", "2 x Polo", 1));
        m.put("SobrePedidoRecojo", new PedidoListoRecojo(id, "FS-000001", destinatario));
        m.put("SobreReembolsoSolicitado", new ReembolsoSolicitado(id, "FS-000001", id, monto, MotivoReembolso.OTRO));
        m.put("SobrePagoConfirmado", new PagoConfirmado(id, id, id, "SIMULADO", "TARJETA", monto, ahora));
        m.put("SobrePagoRechazado", new PagoRechazado(id, "SIMULADO", "Fondos insuficientes"));
        m.put("SobreBoletaEmitida", new BoletaEmitida(id, "B001-00000001", "/api/v1/pagos/FS-000001/boleta"));
        m.put("SobrePagoReembolsado", new PagoReembolsado(id, "FS-000001", id, monto, false, "POSTVENTA", false,
                persona));
        m.put("SobreEntregaActualizada", new EntregaActualizada(id, id, "FS-000001", "DELIVERY_LOCAL", "ENTREGADA", 1,
                LocalDate.of(2026, 10, 20), "Juan", null, null, null, null, null, persona));
        return m;
    }

    @Test
    @DisplayName("CA-0a-06 PedidoRecogido lleva los mismos campos que PedidoListoRecojo (SobrePedidoRecojo)")
    void recogidoComparteEsquema() {
        var destinatario = new DestinatarioEvento("Ana", "Quispe", "12345678", "987654321", "ana@correo.pe");
        UUID id = UUID.randomUUID();

        var recogido = json.readTree(json.writeValueAsString(new PedidoRecogido(id, "FS-000001", destinatario)));
        var listo = json.readTree(json.writeValueAsString(new PedidoListoRecojo(id, "FS-000001", destinatario)));

        assertThat(recogido.propertyNames()).containsExactlyInAnyOrderElementsOf(listo.propertyNames());
    }
}
