package com.fitstore.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.delivery.config.SolicitudInvalidaException;
import com.fitstore.delivery.dto.AsignarRequest;
import com.fitstore.delivery.dto.EntregaDto;
import com.fitstore.delivery.dto.EnvioAgenciaRequest;
import com.fitstore.delivery.dto.ObservacionRequest;
import com.fitstore.delivery.dto.VersionRequest;
import com.fitstore.delivery.model.AccionEntrega;
import com.fitstore.delivery.model.EstadoEntrega;
import com.fitstore.delivery.model.Repartidor;
import com.fitstore.delivery.model.TipoVehiculo;
import com.fitstore.delivery.pruebas.Pedidos;
import com.fitstore.delivery.pruebas.PruebaApi;
import com.fitstore.delivery.pruebas.Sesiones;
import com.fitstore.delivery.repository.RepartidorRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Núcleo: creación de la entrega y avances local y por agencia, con su seguimiento y su evento (CA-6-19 a CA-6-27). */
class EntregaServiceTest extends PruebaApi {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final String TELEFONO_REPARTIDOR = "987654321";

    @Autowired EntregaService servicio;
    @Autowired RepartidorRepository repartidores;

    private final Actor operador = new Actor(UUID.fromString(Sesiones.ID_OPERADOR), "Olga Operadora");

    private Repartidor repartidor(String nombres, boolean activo) {
        return repartidores.save(Repartidor.builder().nombres(nombres).telefono(TELEFONO_REPARTIDOR)
                .vehiculo(TipoVehiculo.MOTO).placa("ABC-123").activo(activo).build());
    }

    private EntregaDto entregaLocal() {
        return servicio.crearDesde(Pedidos.local("FS-000201"));
    }

    private EntregaDto asignada(Repartidor repartidor) {
        EntregaDto nueva = entregaLocal();
        return servicio.asignar(nueva.id(), new AsignarRequest(repartidor.getId(), HOY, nueva.version()), operador);
    }

    private List<String> eventos() {
        // jsonb devuelve el texto con espacios después de los dos puntos: se normaliza a JSON compacto
        return jdbc.queryForList("SELECT payload::text FROM outbox_evento WHERE tipo = 'EntregaActualizada' ORDER BY creado_en",
                String.class).stream().map(texto -> json.readTree(texto).toString()).toList();
    }

    private String ultimoEvento() {
        List<String> todos = eventos();
        return todos.get(todos.size() - 1);
    }

    // --- HU-6-05 ---

    @Test
    @DisplayName("CA-6-19 crea la entrega local en PENDIENTE_ASIGNACION con la copia del destinatario, la dirección y los ítems")
    void creaLaEntregaLocal() {
        EntregaDto entrega = servicio.crearDesde(Pedidos.local("FS-000201"));

        assertThat(entrega.estado()).isEqualTo(EstadoEntrega.PENDIENTE_ASIGNACION);
        assertThat(entrega.codigoPedido()).isEqualTo("FS-000201");
        assertThat(entrega.destinatario().nombres()).isEqualTo("Lucía");
        assertThat(entrega.destinatario().dni()).isEqualTo("45678912");
        assertThat(entrega.direccion().distrito()).isEqualTo("Miraflores");
        assertThat(entrega.direccion().direccion()).isEqualTo("Av. Larco 345 dpto 201");
        assertThat(entrega.resumenItems()).contains("Legging Pro");
        assertThat(entrega.bultos()).isEqualTo(2);
        assertThat(entrega.eventos()).singleElement().satisfies(ev -> {
            assertThat(ev.estado()).isEqualTo(EstadoEntrega.PENDIENTE_ASIGNACION);
            assertThat(ev.actorNombre()).isNull();
        });
        assertThat(eventos()).singleElement().satisfies(json -> {
            assertThat(json).contains("\"estado\":\"PENDIENTE_ASIGNACION\"");
            assertThat(json).contains("\"codigoPedido\":\"FS-000201\"");
        });
    }

    @Test
    @DisplayName("CA-6-19 crea la entrega por agencia en PENDIENTE_ENVIO con la agencia elegida")
    void creaLaEntregaPorAgencia() {
        Long shalom = jdbc.queryForObject("SELECT id FROM agencia WHERE nombre = 'Shalom'", Long.class);

        EntregaDto entrega = servicio.crearDesde(Pedidos.agencia("FS-000202", shalom, "Shalom"));

        assertThat(entrega.estado()).isEqualTo(EstadoEntrega.PENDIENTE_ENVIO);
        assertThat(entrega.agenciaId()).isEqualTo(shalom);
        assertThat(entrega.agenciaNombre()).isEqualTo("Shalom");
        assertThat(entrega.accionesPermitidas()).containsExactly(AccionEntrega.ENVIADA_AGENCIA);
    }

    @Test
    @DisplayName("CA-6-19 si el mismo evento llega dos veces sigue habiendo una sola entrega y un solo evento publicado")
    void esIdempotente() {
        var pedido = Pedidos.local("FS-000203");

        EntregaDto primera = servicio.crearDesde(pedido);
        EntregaDto segunda = servicio.crearDesde(pedido);

        assertThat(segunda.id()).isEqualTo(primera.id());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM entrega WHERE pedido_id = ?", Integer.class, pedido.pedidoId()))
                .isEqualTo(1);
        assertThat(eventos()).hasSize(1);
    }

    // --- HU-6-06 ---

    @Test
    @DisplayName("CA-6-20 asigna un repartidor activo y una fecha: pasa a ASIGNADA con el intento 1")
    void asigna() {
        Repartidor juan = repartidor("Juan Pérez", true);

        EntregaDto entrega = asignada(juan);

        assertThat(entrega.estado()).isEqualTo(EstadoEntrega.ASIGNADA);
        assertThat(entrega.fechaProgramada()).isEqualTo(HOY);
        assertThat(entrega.repartidor().nombres()).isEqualTo("Juan Pérez");
        assertThat(entrega.intento()).isEqualTo(1);
        assertThat(entrega.siguienteAccion()).isEqualTo(AccionEntrega.EN_RUTA);
    }

    @Test
    @DisplayName("CA-6-20 si ya estaba asignada, se reprograma con otro repartidor o fecha sin sumar intentos")
    void reprograma() {
        Repartidor juan = repartidor("Juan Pérez", true);
        Repartidor rosa = repartidor("Rosa Díaz", true);
        EntregaDto asignada = asignada(juan);

        EntregaDto reprogramada = servicio.asignar(asignada.id(),
                new AsignarRequest(rosa.getId(), HOY.plusDays(2), asignada.version()), operador);

        assertThat(reprogramada.estado()).isEqualTo(EstadoEntrega.ASIGNADA);
        assertThat(reprogramada.repartidor().nombres()).isEqualTo("Rosa Díaz");
        assertThat(reprogramada.fechaProgramada()).isEqualTo(HOY.plusDays(2));
        assertThat(reprogramada.intento()).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-6-20 un repartidor inactivo o inexistente y una fecha pasada se rechazan con 400")
    void validaLaAsignacion() {
        Repartidor inactivo = repartidor("Luis Castro", false);
        Repartidor activo = repartidor("Juan Pérez", true);
        EntregaDto nueva = entregaLocal();

        assertThatThrownBy(() -> servicio.asignar(nueva.id(), new AsignarRequest(inactivo.getId(), HOY, nueva.version()), operador))
                .isInstanceOfSatisfying(SolicitudInvalidaException.class,
                        e -> assertThat(e.errores().get(0).campo()).isEqualTo("repartidorId"));
        assertThatThrownBy(() -> servicio.asignar(nueva.id(), new AsignarRequest(99999L, HOY, nueva.version()), operador))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.asignar(nueva.id(),
                new AsignarRequest(activo.getId(), HOY.minusDays(1), nueva.version()), operador))
                .isInstanceOfSatisfying(SolicitudInvalidaException.class,
                        e -> assertThat(e.errores().get(0).campo()).isEqualTo("fechaProgramada"));
        assertThat(servicio.obtener(nueva.id()).estado()).isEqualTo(EstadoEntrega.PENDIENTE_ASIGNACION);
    }

    @Test
    @DisplayName("CA-6-21 En ruta y Entregada quedan en el seguimiento con el usuario y publican EntregaActualizada")
    void enRutaYEntregada() {
        EntregaDto asignada = asignada(repartidor("Juan Pérez", true));

        EntregaDto enRuta = servicio.enRuta(asignada.id(), new VersionRequest(asignada.version()), operador);
        EntregaDto entregada = servicio.entregada(asignada.id(), new VersionRequest(enRuta.version()), operador);

        assertThat(entregada.estado()).isEqualTo(EstadoEntrega.ENTREGADA);
        assertThat(entregada.accionesPermitidas()).isEmpty();
        assertThat(entregada.siguienteAccion()).isNull();
        assertThat(entregada.eventos()).extracting(e -> e.estado().name())
                .containsExactly("PENDIENTE_ASIGNACION", "ASIGNADA", "EN_RUTA", "ENTREGADA");
        assertThat(entregada.eventos().get(2).actorNombre()).isEqualTo("Olga Operadora");
        assertThat(eventos()).hasSize(4);
        assertThat(ultimoEvento()).contains("\"estado\":\"ENTREGADA\"");
    }

    @Test
    @DisplayName("CA-6-22 No se pudo entregar exige la observación, pasa a FALLIDA y reprogramar vuelve a ASIGNADA con el intento 2")
    void fallidaYReprogramacion() {
        Repartidor juan = repartidor("Juan Pérez", true);
        EntregaDto asignada = asignada(juan);
        EntregaDto enRuta = servicio.enRuta(asignada.id(), new VersionRequest(asignada.version()), operador);

        assertThatThrownBy(() -> servicio.fallida(enRuta.id(), new ObservacionRequest("  ", enRuta.version()), operador))
                .isInstanceOf(SolicitudInvalidaException.class);
        EntregaDto fallida = servicio.fallida(enRuta.id(), new ObservacionRequest("No había nadie", enRuta.version()), operador);

        assertThat(fallida.estado()).isEqualTo(EstadoEntrega.FALLIDA);
        assertThat(fallida.eventos().get(fallida.eventos().size() - 1).observacion()).isEqualTo("No había nadie");
        assertThat(ultimoEvento()).contains("\"observacion\":\"No había nadie\"");

        EntregaDto reprogramada = servicio.asignar(fallida.id(),
                new AsignarRequest(juan.getId(), HOY.plusDays(1), fallida.version()), operador);
        assertThat(reprogramada.estado()).isEqualTo(EstadoEntrega.ASIGNADA);
        assertThat(reprogramada.intento()).isEqualTo(2);
        assertThat(reprogramada.eventos().get(reprogramada.eventos().size() - 1).intento()).isEqualTo(2);
    }

    @Test
    @DisplayName("CA-6-23 una transición no permitida da 409 con el estado actual y no cambia nada")
    void transicionNoPermitida() {
        EntregaDto asignada = asignada(repartidor("Juan Pérez", true));
        EntregaDto enRuta = servicio.enRuta(asignada.id(), new VersionRequest(asignada.version()), operador);
        EntregaDto entregada = servicio.entregada(asignada.id(), new VersionRequest(enRuta.version()), operador);
        int antes = eventos().size();

        assertThatThrownBy(() -> servicio.enRuta(entregada.id(), new VersionRequest(entregada.version()), operador))
                .isInstanceOfSatisfying(ConflictoNegocioException.class,
                        e -> assertThat(e.detalles()).containsEntry("estadoActual", "ENTREGADA"));
        // Una acción de agencia en una entrega local
        EntregaDto otra = entregaLocal2();
        assertThatThrownBy(() -> servicio.disponibleAgencia(otra.id(), new VersionRequest(otra.version()), operador))
                .isInstanceOf(ConflictoNegocioException.class);
        assertThatThrownBy(() -> servicio.enviadaAgencia(otra.id(),
                new EnvioAgenciaRequest(null, "SH-1234", null, otra.version()), operador))
                .isInstanceOf(ConflictoNegocioException.class);

        assertThat(servicio.obtener(entregada.id()).estado()).isEqualTo(EstadoEntrega.ENTREGADA);
        assertThat(eventos()).hasSize(antes + 1); // solo el alta de la otra entrega
    }

    private EntregaDto entregaLocal2() {
        return servicio.crearDesde(Pedidos.local("FS-000299"));
    }

    @Test
    @DisplayName("CA-6-24 si otro operador cambió la entrega, el avance con la versión vieja da 409 'recarga la página'")
    void versionDesactualizada() {
        EntregaDto asignada = asignada(repartidor("Juan Pérez", true));
        servicio.enRuta(asignada.id(), new VersionRequest(asignada.version()), operador);

        assertThatThrownBy(() -> servicio.enRuta(asignada.id(), new VersionRequest(asignada.version()), operador))
                .isInstanceOfSatisfying(ConflictoNegocioException.class, e -> {
                    assertThat(e.codigo()).isEqualTo("CONFLICTO_VERSION");
                    assertThat(e.getMessage()).isEqualTo("La entrega cambió; recarga la página");
                });
    }

    @Test
    @DisplayName("CA-6-25 EntregaActualizada lleva el nombre del repartidor y la fecha programada, nunca su teléfono")
    void eventoSinTelefonoDelRepartidor() {
        asignada(repartidor("Juan Pérez", true));

        String json = ultimoEvento();

        assertThat(json).contains("\"repartidorNombre\":\"Juan Pérez\"");
        assertThat(json).contains("\"fechaProgramada\":\"2026-10-19\"");
        assertThat(json).contains("\"intento\":1");
        assertThat(json).doesNotContain(TELEFONO_REPARTIDOR).doesNotContain("telefono");
        assertThat(json).contains("\"email\":\"lucia@correo.pe\"");
    }

    // --- HU-6-07 ---

    @Test
    @DisplayName("CA-6-26 registrar el envío exige el código; pasa a ENVIADO_AGENCIA con la agencia, el código y la URL de rastreo")
    void enviaPorAgencia() {
        Long shalom = jdbc.queryForObject("SELECT id FROM agencia WHERE nombre = 'Shalom'", Long.class);
        Long olva = jdbc.queryForObject("SELECT id FROM agencia WHERE nombre = 'Olva Courier'", Long.class);
        EntregaDto nueva = servicio.crearDesde(Pedidos.agencia("FS-000204", shalom, "Shalom"));

        assertThatThrownBy(() -> servicio.enviadaAgencia(nueva.id(), new EnvioAgenciaRequest(null, " ", null, nueva.version()), operador))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.enviadaAgencia(nueva.id(), new EnvioAgenciaRequest(99999L, "SH-9", null, nueva.version()), operador))
                .isInstanceOf(SolicitudInvalidaException.class);

        EntregaDto enviada = servicio.enviadaAgencia(nueva.id(),
                new EnvioAgenciaRequest(olva, "OL-778899", "4821", nueva.version()), operador);

        assertThat(enviada.estado()).isEqualTo(EstadoEntrega.ENVIADO_AGENCIA);
        assertThat(enviada.agenciaNombre()).isEqualTo("Olva Courier");
        assertThat(enviada.codigoSeguimiento()).isEqualTo("OL-778899");
        assertThat(enviada.claveRecojo()).isEqualTo("4821");
        assertThat(enviada.urlSeguimiento()).isEqualTo("https://rastreo.ejemplo.pe/olva?codigo=OL-778899");
        String json = ultimoEvento();
        assertThat(json).contains("\"estado\":\"ENVIADO_AGENCIA\"").contains("\"agenciaNombre\":\"Olva Courier\"")
                .contains("\"codigoSeguimiento\":\"OL-778899\"")
                .contains("\"urlSeguimiento\":\"https://rastreo.ejemplo.pe/olva?codigo=OL-778899\"");
    }

    @Test
    @DisplayName("CA-6-26 sin cambiar la agencia se conserva la elegida por el cliente y la clave de recojo es opcional")
    void conservaLaAgenciaDelCliente() {
        Long shalom = jdbc.queryForObject("SELECT id FROM agencia WHERE nombre = 'Shalom'", Long.class);
        EntregaDto nueva = servicio.crearDesde(Pedidos.agencia("FS-000205", shalom, "Shalom"));

        EntregaDto enviada = servicio.enviadaAgencia(nueva.id(), new EnvioAgenciaRequest(null, "SH-1234", null, nueva.version()), operador);

        assertThat(enviada.agenciaNombre()).isEqualTo("Shalom");
        assertThat(enviada.claveRecojo()).isNull();
    }

    @Test
    @DisplayName("CA-6-27 Disponible en agencia y luego Recogido pasan a DISPONIBLE_EN_AGENCIA y ENTREGADA, con su evento")
    void disponibleYRecogido() {
        Long shalom = jdbc.queryForObject("SELECT id FROM agencia WHERE nombre = 'Shalom'", Long.class);
        EntregaDto nueva = servicio.crearDesde(Pedidos.agencia("FS-000206", shalom, "Shalom"));
        EntregaDto enviada = servicio.enviadaAgencia(nueva.id(), new EnvioAgenciaRequest(null, "SH-1234", "99", nueva.version()), operador);

        EntregaDto disponible = servicio.disponibleAgencia(enviada.id(), new VersionRequest(enviada.version()), operador);
        assertThat(disponible.estado()).isEqualTo(EstadoEntrega.DISPONIBLE_EN_AGENCIA);
        assertThat(ultimoEvento()).contains("\"estado\":\"DISPONIBLE_EN_AGENCIA\"").contains("\"claveRecojo\":\"99\"");

        EntregaDto recogido = servicio.entregada(disponible.id(), new VersionRequest(disponible.version()), operador);
        assertThat(recogido.estado()).isEqualTo(EstadoEntrega.ENTREGADA);
        assertThat(ultimoEvento()).contains("\"estado\":\"ENTREGADA\"");
        assertThat(eventos()).hasSize(4);
    }
}
