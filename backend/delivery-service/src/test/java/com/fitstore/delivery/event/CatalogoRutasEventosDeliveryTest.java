package com.fitstore.delivery.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.FileSystemResource;

/** Rutas de los eventos de delivery (SPEC-0a I4): tabla fija y contraste con el AsyncAPI y con el YAML del config-repo. */
class CatalogoRutasEventosDeliveryTest {

    private static final Path ASYNCAPI = Path.of("../../contracts/asyncapi/fitstore-eventos.yaml");
    private static final Path CONFIG = Path.of("../../infra/config-repo/delivery-service.yml");

    private final CatalogoRutasEventosDelivery catalogo = new CatalogoRutasEventosDelivery();

    @Test
    @DisplayName("CA-0a-06 cada evento que publica delivery tiene su routing key (tabla fija)")
    void tablaFija() {
        assertThat(catalogo.routingKey("EntregaActualizada")).isEqualTo("delivery.entrega.actualizada");
    }

    @Test
    @DisplayName("CA-0a-06 un tipo que delivery no publica es un error")
    void tipoDesconocido() {
        assertThatThrownBy(() -> catalogo.routingKey("PedidoPagado")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PedidoPagado");
    }

    @Test
    @DisplayName("CA-0a-06 las routing keys que publica delivery coinciden con los canales del AsyncAPI")
    @SuppressWarnings("unchecked")
    void coincideConElAsyncApi() throws IOException {
        Map<String, Object> asyncapi = leer(ASYNCAPI);
        Map<String, Map<String, Object>> operaciones = (Map<String, Map<String, Object>>) asyncapi.get("operations");
        Map<String, Map<String, Object>> canales = (Map<String, Map<String, Object>>) asyncapi.get("channels");

        List<String> publicadas = operaciones.entrySet().stream()
                .filter(o -> o.getKey().startsWith("delivery") && "send".equals(o.getValue().get("action")))
                .map(o -> direccionDelCanal(o.getValue(), canales)).toList();

        assertThat(publicadas).containsExactly(catalogo.routingKey("EntregaActualizada"));
    }

    @Test
    @DisplayName("CA-0a-06 la cola de delivery se enlaza solo a la routing key que consume según el AsyncAPI")
    @SuppressWarnings("unchecked")
    void enlacesCoincidenConElAsyncApi() throws IOException {
        Map<String, Object> asyncapi = leer(ASYNCAPI);
        Map<String, Map<String, Object>> operaciones = (Map<String, Map<String, Object>>) asyncapi.get("operations");
        Map<String, Map<String, Object>> canales = (Map<String, Map<String, Object>>) asyncapi.get("channels");
        List<String> consumidas = operaciones.entrySet().stream()
                .filter(o -> o.getKey().startsWith("delivery") && "receive".equals(o.getValue().get("action")))
                .map(o -> direccionDelCanal(o.getValue(), canales)).toList();

        Map<String, Object> fitstore = (Map<String, Object>) leer(CONFIG).get("fitstore");
        Map<String, Object> eventos = (Map<String, Object>) fitstore.get("eventos");

        assertThat(eventos.get("servicio")).isEqualTo("delivery");
        assertThat((List<String>) eventos.get("enlaces")).containsExactlyElementsOf(consumidas)
                .containsExactly("order.pedido.listo-despacho");
    }

    @SuppressWarnings("unchecked")
    private static String direccionDelCanal(Map<String, Object> operacion, Map<String, Map<String, Object>> canales) {
        String referencia = ((Map<String, String>) operacion.get("channel")).get("$ref");
        String canal = referencia.substring(referencia.lastIndexOf('/') + 1);
        return (String) canales.get(canal).get("address");
    }

    private static Map<String, Object> leer(Path ruta) throws IOException {
        YamlMapFactoryBean yaml = new YamlMapFactoryBean();
        yaml.setResources(new FileSystemResource(ruta));
        Map<String, Object> contenido = yaml.getObject();
        if (contenido == null) {
            throw new IOException("No se pudo leer " + ruta);
        }
        return contenido;
    }
}
