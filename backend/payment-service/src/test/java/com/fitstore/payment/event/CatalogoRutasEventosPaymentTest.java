package com.fitstore.payment.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.MotivoReembolso;
import com.fitstore.payment.model.Proveedor;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
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

/** Cada evento que publica payment tiene su routing key, y todo coincide con los canales y esquemas del AsyncAPI. */
class CatalogoRutasEventosPaymentTest {

    private static final Path ASYNCAPI = Path.of("../../contracts/asyncapi/fitstore-eventos.yaml");
    private static final Path CONFIG = Path.of("../../infra/config-repo/payment-service.yml");

    /** Tabla fija: tipo publicado por payment y su routing key (micro 6). */
    private static final Map<String, String> PUBLICADOS = Map.of(
            "PagoConfirmado", "payment.pago.confirmado",
            "PagoRechazado", "payment.pago.rechazado",
            "BoletaEmitida", "payment.boleta.emitida",
            "PagoReembolsado", "payment.reembolso.completado",
            "ReembolsoFallido", "payment.reembolso.fallido");

    /** Routing keys a las que se enlaza payment.eventos (micro 10.6). */
    private static final Set<String> CONSUMIDAS = Set.of(
            "order.pedido.creado", "order.pedido.cancelado", "order.pedido.expirado",
            "order.pago.no-aplicable", "order.reembolso.solicitado");

    private final CatalogoRutasEventosPayment rutas = new CatalogoRutasEventosPayment();
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    @DisplayName("CA-0a-06 cada evento que publica payment tiene su routing key")
    void routingKeys() {
        PUBLICADOS.forEach((tipo, clave) -> assertThat(rutas.routingKey(tipo)).as(tipo).isEqualTo(clave));
    }

    @Test
    @DisplayName("CA-0a-06 un tipo de evento desconocido se rechaza")
    void tipoDesconocido() {
        assertThatThrownBy(() -> rutas.routingKey("PedidoPagado"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PedidoPagado");
    }

    @Test
    @DisplayName("CA-0a-06 las routing keys coinciden con las direcciones de los canales del AsyncAPI")
    @SuppressWarnings("unchecked")
    void coincideConElAsyncApi() throws Exception {
        Map<String, Object> raiz = new Yaml().load(Files.readString(ASYNCAPI));
        Map<String, Map<String, Object>> canales = (Map<String, Map<String, Object>>) raiz.get("channels");
        Map<String, Map<String, Object>> operaciones = (Map<String, Map<String, Object>>) raiz.get("operations");

        for (String tipo : PUBLICADOS.keySet()) {
            String canal = Character.toLowerCase(tipo.charAt(0)) + tipo.substring(1);
            assertThat(rutas.routingKey(tipo)).as(tipo).isEqualTo(canales.get(canal).get("address"));
            Map<String, Object> operacion = operaciones.get("paymentPublica" + tipo);
            assertThat(operacion).as("operación de " + tipo).isNotNull();
            assertThat(operacion.get("action")).isEqualTo("send");
        }
    }

    @Test
    @DisplayName("CA-0a-06 las routing keys que consume payment existen como canales con operación receive de payment")
    @SuppressWarnings("unchecked")
    void consumidasEnElAsyncApi() throws Exception {
        Map<String, Object> raiz = new Yaml().load(Files.readString(ASYNCAPI));
        Map<String, Map<String, Object>> canales = (Map<String, Map<String, Object>>) raiz.get("channels");
        Map<String, Map<String, Object>> operaciones = (Map<String, Map<String, Object>>) raiz.get("operations");

        Set<String> recibidas = operaciones.entrySet().stream()
                .filter(o -> o.getKey().startsWith("paymentRecibe") && "receive".equals(o.getValue().get("action")))
                .map(o -> ((String) ((Map<String, Object>) o.getValue().get("channel")).get("$ref"))
                        .substring("#/channels/".length()))
                .map(canal -> (String) canales.get(canal).get("address"))
                .collect(Collectors.toSet());

        assertThat(recibidas).isEqualTo(CONSUMIDAS);
    }

    @Test
    @DisplayName("CA-0a-06 fitstore.eventos.enlaces de payment-service.yml son las routing keys que consume")
    @SuppressWarnings("unchecked")
    void enlacesDelYaml() throws Exception {
        Map<String, Object> raiz = new Yaml().load(Files.readString(CONFIG));
        Map<String, Object> fitstore = (Map<String, Object>) raiz.get("fitstore");
        Map<String, Object> eventos = (Map<String, Object>) fitstore.get("eventos");

        assertThat(eventos.get("servicio")).isEqualTo("payment");
        assertThat((List<String>) eventos.get("enlaces")).containsExactlyInAnyOrderElementsOf(CONSUMIDAS);
    }

    @Test
    @DisplayName("CA-0a-06 los campos de cada evento publicado coinciden con el AsyncAPI")
    void camposPublicados() throws Exception {
        UUID id = UUID.randomUUID();
        Instant ahora = Instant.now();
        BigDecimal monto = new BigDecimal("59.90");
        Map<String, Object> eventos = new LinkedHashMap<>();
        eventos.put("SobrePagoConfirmado",
                new PagoConfirmado(id, id, id, Proveedor.SIMULADO, Medio.TARJETA, monto, ahora));
        eventos.put("SobrePagoRechazado", new PagoRechazado(id, Proveedor.CULQI, "Fondos insuficientes"));
        eventos.put("SobreBoletaEmitida", new BoletaEmitida(id, "B001-00000001", "/api/v1/pagos/FS-000001/boleta"));
        eventos.put("SobrePagoReembolsado", new PagoReembolsado(id, "FS-000001", id, monto, true,
                MotivoReembolso.POSTVENTA, false, new Persona("Ana", "ana@correo.pe")));
        eventos.put("SobreReembolsoFallido", new ReembolsoFallido(id, "FS-000001", monto, "timeout", 4));

        verificarCampos(eventos);
    }

    @Test
    @DisplayName("CA-0a-06 los campos de cada evento consumido coinciden con el AsyncAPI")
    void camposConsumidos() throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> eventos = new LinkedHashMap<>();
        eventos.put("SobrePedidoCreado", new PedidoCreado(id, "FS-000001", id, null, ModalidadEntrega.RECOJO, null,
                new ArrayList<>(), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, Instant.now()));
        eventos.put("SobrePedidoCancelado",
                new PedidoCancelado(id, "FS-000001", true, null, ActorCancelacion.CLIENTE, List.of()));
        eventos.put("SobrePedidoExpirado", new PedidoExpirado(id, "FS-000001"));
        eventos.put("SobrePagoNoAplicable", new PagoNoAplicable(id, id, MotivoNoAplicable.PEDIDO_EXPIRADO));
        eventos.put("SobreReembolsoSolicitado",
                new ReembolsoSolicitado(id, "FS-000001", id, BigDecimal.TEN, MotivoPostventa.DEFECTUOSO));

        verificarCampos(eventos);
    }

    /** Los campos serializados del record son exactamente las propiedades de {@code datos} del esquema. */
    @SuppressWarnings("unchecked")
    private void verificarCampos(Map<String, Object> eventos) throws Exception {
        Map<String, Object> raiz = new Yaml().load(Files.readString(ASYNCAPI));
        Map<String, Map<String, Object>> esquemas =
                (Map<String, Map<String, Object>>) ((Map<String, Object>) raiz.get("components")).get("schemas");

        for (var e : eventos.entrySet()) {
            List<Map<String, Object>> allOf = (List<Map<String, Object>>) esquemas.get(e.getKey()).get("allOf");
            Map<String, Object> props = (Map<String, Object>) allOf.get(1).get("properties");
            Map<String, Object> datos = (Map<String, Object>) props.get("datos");
            Set<String> requeridos = Set.copyOf((List<String>) datos.get("required"));
            Set<String> permitidos = Set.copyOf(((Map<String, Object>) datos.get("properties")).keySet());

            JsonNode nodo = json.readTree(json.writeValueAsString(e.getValue()));
            Set<String> campos = nodo.propertyNames().stream().collect(Collectors.toSet());
            assertThat(campos).as(e.getKey()).containsAll(requeridos).isSubsetOf(permitidos);
        }
    }
}
