package com.fitstore.notification.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Las 17 routing keys que notification consume (micro 6 y 10.6): las de {@code fitstore.eventos.enlaces} deben
 * coincidir con una tabla fija y con los canales del AsyncAPI que tienen la operación {@code notificationRecibe*}.
 */
class RutasEventosTest {

    private static final Path RAIZ = Path.of("..", "..");

    private static final List<String> ESPERADAS = List.of(
            "auth.cliente.registrado", "auth.password.reset-solicitado", "auth.password.cambiado",
            "catalog.stock.bajo", "catalog.stock.no-confirmado",
            "order.pedido.creado", "order.pedido.pagado", "order.pedido.cancelado",
            "order.pedido.listo-recojo", "order.pedido.recogido",
            "payment.boleta.emitida", "payment.reembolso.completado", "payment.reembolso.fallido",
            "delivery.entrega.actualizada",
            "settings.configuracion.actualizada", "settings.reclamo.registrado", "settings.reclamo.respondido");

    private static Map<String, Object> yaml(Path ruta) throws IOException {
        try (InputStream entrada = Files.newInputStream(ruta)) {
            return new Yaml().load(entrada);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> enlacesDelYaml() throws IOException {
        Map<String, Object> raiz = yaml(RAIZ.resolve("infra/config-repo/notification-service.yml"));
        Map<String, Object> fitstore = (Map<String, Object>) raiz.get("fitstore");
        Map<String, Object> eventos = (Map<String, Object>) fitstore.get("eventos");
        assertThat(eventos.get("servicio")).isEqualTo("notification");
        return new ArrayList<>((List<String>) eventos.get("enlaces"));
    }

    @Test
    @DisplayName("CA-0a-06 las routing keys que consume coinciden con la tabla fija")
    void coincidenConLaTablaFija() throws IOException {
        List<String> enlaces = enlacesDelYaml();
        assertThat(enlaces).hasSize(17).doesNotHaveDuplicates();
        assertThat(new TreeSet<>(enlaces)).isEqualTo(new TreeSet<>(ESPERADAS));
    }

    @Test
    @DisplayName("CA-0a-06 las routing keys que consume coinciden con los canales del AsyncAPI")
    @SuppressWarnings("unchecked")
    void coincidenConElAsyncApi() throws IOException {
        Map<String, Object> asyncapi = yaml(RAIZ.resolve("contracts/asyncapi/fitstore-eventos.yaml"));
        Map<String, Map<String, Object>> canales = (Map<String, Map<String, Object>>) asyncapi.get("channels");
        Map<String, Map<String, Object>> operaciones = (Map<String, Map<String, Object>>) asyncapi.get("operations");

        TreeSet<String> recibidas = new TreeSet<>();
        operaciones.forEach((nombre, operacion) -> {
            if (nombre.startsWith("notificationRecibe")) {
                String ref = (String) ((Map<String, Object>) operacion.get("channel")).get("$ref");
                String canal = ref.substring(ref.lastIndexOf('/') + 1);
                recibidas.add((String) canales.get(canal).get("address"));
            }
        });

        assertThat(recibidas).hasSize(17);
        assertThat(new TreeSet<>(enlacesDelYaml())).isEqualTo(recibidas);
    }
}
