package com.fitstore.settings.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.common.event.CatalogoRutasEventos;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CatalogoRutasSettingsTest {

    private final CatalogoRutasEventos rutas = new CatalogoRutasSettings();

    @Test
    @DisplayName("CA-0a-06 cada evento que publica settings tiene su routing key del AsyncAPI")
    void routingKeysDeLosEventosQuePublica() {
        assertThat(rutas.routingKey("ConfiguracionActualizada")).isEqualTo("settings.configuracion.actualizada");
        assertThat(rutas.routingKey("ReclamoRegistrado")).isEqualTo("settings.reclamo.registrado");
        assertThat(rutas.routingKey("ReclamoRespondido")).isEqualTo("settings.reclamo.respondido");
    }

    @Test
    @DisplayName("CA-0a-06 las routing keys coinciden con los canales de contracts/asyncapi/fitstore-eventos.yaml")
    void coincidenConElAsyncApi() throws Exception {
        Path asyncapi = Path.of("../../contracts/asyncapi/fitstore-eventos.yaml");
        String yaml = Files.readString(asyncapi);
        Matcher m = Pattern.compile("address: (settings\\.[a-z.]+)").matcher(yaml);
        List<String> canales = m.results().map(r -> r.group(1)).toList();

        assertThat(canales).containsExactlyInAnyOrder(
                rutas.routingKey("ConfiguracionActualizada"),
                rutas.routingKey("ReclamoRegistrado"),
                rutas.routingKey("ReclamoRespondido"));
    }

    @Test
    @DisplayName("CA-0a-06 un tipo de evento desconocido se rechaza")
    void tipoDesconocido() {
        assertThatThrownBy(() -> rutas.routingKey("PedidoPagado")).isInstanceOf(IllegalArgumentException.class);
    }
}
