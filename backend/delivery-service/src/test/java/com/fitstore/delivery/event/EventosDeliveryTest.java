package com.fitstore.delivery.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Forma de los eventos de delivery frente al AsyncAPI (K12): el contenido {@code datos} del sobre. */
class EventosDeliveryTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    @DisplayName("CA-0a-06 PedidoListoDespacho se lee desde el contenido del AsyncAPI e ignora campos desconocidos")
    void leePedidoListoDespacho() {
        String pedidoId = UUID.randomUUID().toString();
        String datos = """
                {"pedidoId":"%s","codigo":"FS-2026-000123","modalidad":"AGENCIA",
                 "destinatario":{"nombres":"Ana","apellidos":"Quispe","dni":"12345678","telefono":"999888777","email":"ana@correo.pe","campoNuevo":1},
                 "direccion":{"ubigeo":"040101","departamento":"Arequipa","provincia":"Arequipa","distrito":"Arequipa",
                              "direccion":"Calle Mercaderes 100","referencia":null},
                 "agenciaId":2,"agenciaNombre":"Olva Courier","resumenItems":"2 x Polo M","bultos":2,"campoFuturo":"x"}
                """.formatted(pedidoId);

        PedidoListoDespacho evento = json.readValue(datos, PedidoListoDespacho.class);

        assertThat(evento.pedidoId()).hasToString(pedidoId);
        assertThat(evento.codigo()).isEqualTo("FS-2026-000123");
        assertThat(evento.modalidad()).isEqualTo("AGENCIA");
        assertThat(evento.destinatario().nombres()).isEqualTo("Ana");
        assertThat(evento.destinatario().dni()).isEqualTo("12345678");
        assertThat(evento.destinatario().email()).isEqualTo("ana@correo.pe");
        assertThat(evento.direccion().ubigeo()).isEqualTo("040101");
        assertThat(evento.direccion().referencia()).isNull();
        assertThat(evento.agenciaId()).isEqualTo(2L);
        assertThat(evento.agenciaNombre()).isEqualTo("Olva Courier");
        assertThat(evento.resumenItems()).isEqualTo("2 x Polo M");
        assertThat(evento.bultos()).isEqualTo(2);
    }

    @Test
    @DisplayName("CA-0a-06 EntregaActualizada se escribe con los nombres y tipos del AsyncAPI")
    void escribeEntregaActualizada() {
        UUID entregaId = UUID.randomUUID();
        UUID pedidoId = UUID.randomUUID();
        EntregaActualizada evento = new EntregaActualizada(entregaId, pedidoId, "FS-2026-000123", "DELIVERY_LOCAL",
                "ASIGNADA", 1, LocalDate.of(2026, 10, 22), "Juan Pérez", null, null, null, null, null,
                new EntregaActualizada.Persona("Ana Quispe", "ana@correo.pe"));

        JsonNode nodo = json.valueToTree(evento);

        assertThat(nodo.get("entregaId").asString()).isEqualTo(entregaId.toString());
        assertThat(nodo.get("pedidoId").asString()).isEqualTo(pedidoId.toString());
        assertThat(nodo.get("codigoPedido").asString()).isEqualTo("FS-2026-000123");
        assertThat(nodo.get("modalidad").asString()).isEqualTo("DELIVERY_LOCAL");
        assertThat(nodo.get("estado").asString()).isEqualTo("ASIGNADA");
        assertThat(nodo.get("intento").asInt()).isEqualTo(1);
        assertThat(nodo.get("fechaProgramada").asString()).isEqualTo("2026-10-22");
        assertThat(nodo.get("repartidorNombre").asString()).isEqualTo("Juan Pérez");
        assertThat(nodo.has("agenciaNombre")).isTrue();
        assertThat(nodo.get("agenciaNombre").isNull()).isTrue();
        assertThat(nodo.get("destinatario").get("nombre").asString()).isEqualTo("Ana Quispe");
        assertThat(nodo.get("destinatario").get("email").asString()).isEqualTo("ana@correo.pe");
        assertThat(nodo.has("telefonoRepartidor")).isFalse();
    }
}
