package com.fitstore.settings.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Los records de los eventos serializan con los nombres de campo del AsyncAPI (sobre K12, campo {@code datos}). */
class EventosSettingsTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    @DisplayName("CA-0a-06 ConfiguracionActualizada lleva version, secciones, configuracion y correosAlertas")
    void configuracionActualizada() {
        ConfiguracionActualizada evento = new ConfiguracionActualizada(4, List.of("TEMA", "IMAGENES"),
                Map.of("tema", Map.of("colorPrimario", "#C9B28C")), List.of("alertas@tienda.pe"));

        JsonNode n = json.valueToTree(evento);

        assertThat(n.get("version").asInt()).isEqualTo(4);
        assertThat(n.get("secciones").get(1).asString()).isEqualTo("IMAGENES");
        assertThat(n.get("configuracion").get("tema").get("colorPrimario").asString()).isEqualTo("#C9B28C");
        assertThat(n.get("correosAlertas").get(0).asString()).isEqualTo("alertas@tienda.pe");
    }

    @Test
    @DisplayName("CA-0a-06 ReclamoRegistrado lleva los campos del contrato")
    void reclamoRegistrado() {
        ReclamoRegistrado evento = new ReclamoRegistrado("RC-2026-000001", "RECLAMO",
                new PersonaEvento("María Quispe", "maria@correo.pe"),
                Map.of("tipo", "PRODUCTO", "descripcion", "Legging"), "Costura abierta", "Cambio",
                LocalDate.of(2026, 3, 20), Instant.parse("2026-02-27T15:00:00Z"));

        JsonNode n = json.valueToTree(evento);

        assertThat(n.get("numero").asString()).isEqualTo("RC-2026-000001");
        assertThat(n.get("consumidor").get("email").asString()).isEqualTo("maria@correo.pe");
        assertThat(n.get("pedidoConsumidor").asString()).isEqualTo("Cambio");
        assertThat(n.get("fechaLimite").asString()).isEqualTo("2026-03-20");
        assertThat(n.get("creadoEn").asString()).isEqualTo("2026-02-27T15:00:00Z");
    }

    @Test
    @DisplayName("CA-0a-06 ReclamoRespondido lleva los campos del contrato")
    void reclamoRespondido() {
        ReclamoRespondido evento = new ReclamoRespondido("RC-2026-000001",
                new PersonaEvento("María Quispe", "maria@correo.pe"), "Procedemos con el cambio.",
                Instant.parse("2026-03-01T09:00:00Z"));

        JsonNode n = json.valueToTree(evento);

        assertThat(n.get("numero").asString()).isEqualTo("RC-2026-000001");
        assertThat(n.get("respuesta").asString()).isEqualTo("Procedemos con el cambio.");
        assertThat(n.get("respondidoEn").asString()).isEqualTo("2026-03-01T09:00:00Z");
    }
}
