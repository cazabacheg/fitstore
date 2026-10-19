package com.fitstore.auth.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.auth.model.MotivoToken;
import com.fitstore.auth.model.TipoCuenta;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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

/** Cada evento que publica auth tiene su routing key, y coincide con el canal y el payload del AsyncAPI. */
class CatalogoRutasEventosAuthTest {

    private static final Path ASYNCAPI = Path.of("../../contracts/asyncapi/fitstore-eventos.yaml");

    private final CatalogoRutasEventosAuth rutas = new CatalogoRutasEventosAuth();
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    @DisplayName("CA-0a-06 cada evento que publica auth tiene su routing key")
    void routingKeys() {
        assertThat(rutas.routingKey("ClienteRegistrado")).isEqualTo("auth.cliente.registrado");
        assertThat(rutas.routingKey("PasswordResetSolicitado")).isEqualTo("auth.password.reset-solicitado");
        assertThat(rutas.routingKey("PasswordCambiado")).isEqualTo("auth.password.cambiado");
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

        for (String tipo : List.of("ClienteRegistrado", "PasswordResetSolicitado", "PasswordCambiado")) {
            String canal = Character.toLowerCase(tipo.charAt(0)) + tipo.substring(1);
            assertThat(rutas.routingKey(tipo)).isEqualTo(canales.get(canal).get("address"));
        }
    }

    @Test
    @DisplayName("CA-0a-06 los campos de cada evento coinciden con los requeridos por el AsyncAPI")
    @SuppressWarnings("unchecked")
    void camposCoincidenConElAsyncApi() throws Exception {
        Map<String, Object> raiz = new Yaml().load(Files.readString(ASYNCAPI));
        Map<String, Map<String, Object>> esquemas =
                (Map<String, Map<String, Object>>) ((Map<String, Object>) raiz.get("components")).get("schemas");
        UUID id = UUID.randomUUID();
        Instant ahora = Instant.now();
        Map<String, Object> eventos = Map.of(
                "SobreClienteRegistrado", new ClienteRegistrado(id, "a@b.pe", "Ana"),
                "SobrePasswordResetSolicitado", new PasswordResetSolicitado(id, TipoCuenta.CLIENTE, "a@b.pe", "Ana",
                        MotivoToken.RECUPERACION, "http://x/restablecer?t=1", ahora),
                "SobrePasswordCambiado", new PasswordCambiado(id, TipoCuenta.STAFF, "a@b.pe", "Ana"));

        for (var e : eventos.entrySet()) {
            List<Map<String, Object>> allOf = (List<Map<String, Object>>) esquemas.get(e.getKey()).get("allOf");
            Map<String, Object> props = (Map<String, Object>) allOf.get(1).get("properties");
            Map<String, Object> datos = (Map<String, Object>) props.get("datos");
            Set<String> requeridos = Set.copyOf((List<String>) datos.get("required"));

            JsonNode nodo = json.readTree(json.writeValueAsString(e.getValue()));
            Set<String> campos = nodo.propertyNames().stream().collect(Collectors.toSet());
            assertThat(campos).as(e.getKey()).isEqualTo(requeridos);
        }
    }
}
