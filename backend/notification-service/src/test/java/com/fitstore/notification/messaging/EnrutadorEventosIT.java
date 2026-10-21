package com.fitstore.notification.messaging;

import static com.fitstore.notification.pruebas.EventosDePrueba.CODIGO;
import static com.fitstore.notification.pruebas.EventosDePrueba.PEDIDO;
import static com.fitstore.notification.pruebas.EventosDePrueba.entrega;
import static com.fitstore.notification.pruebas.EventosDePrueba.pedidoCreado;
import static com.fitstore.notification.pruebas.EventosDePrueba.persona;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fitstore.common.event.EventosProperties;
import com.fitstore.common.event.SobreEvento;
import com.fitstore.notification.model.EnvioCorreo;
import com.fitstore.notification.model.EstadoEnvio;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.CorreoRenderizado;
import com.fitstore.notification.plantillas.MarcaService;
import com.fitstore.notification.plantillas.RenderizadorCorreo;
import com.fitstore.notification.plantillas.TextosPlantilla;
import com.fitstore.notification.pruebas.EventosDePrueba;
import com.fitstore.notification.pruebas.LimpiezaBd;
import com.fitstore.notification.pruebas.PostgresInicializador;
import com.fitstore.notification.pruebas.RabbitInicializador;
import com.fitstore.notification.repository.EnvioCorreoRepository;
import com.fitstore.notification.repository.PlantillaRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Cada evento llega por RabbitMQ (testcontainers) a su manejador y deja el envío que corresponde (SPEC-7 sección 7,
 * CA-7-01 a CA-7-07).
 */
@SpringBootTest
@ActiveProfiles({"test", "sin-config-server"})
@ContextConfiguration(initializers = {PostgresInicializador.class, RabbitInicializador.class})
class EnrutadorEventosIT {

    private static final String CLIENTE = "ana.quispe@fitstore.test";
    private static final String ALERTAS_1 = "dueno@fitstore.test";
    private static final String ALERTAS_2 = "almacen@fitstore.test";

    @Autowired RabbitTemplate rabbit;
    @Autowired JdbcTemplate jdbc;
    @Autowired EventosProperties eventos;
    @Autowired EnvioCorreoRepository envios;
    @Autowired PlantillaRepository plantillas;
    @Autowired RenderizadorCorreo renderizador;
    @Autowired MarcaService marca;

    @BeforeEach
    void limpiar() {
        LimpiezaBd.limpiar(jdbc);
    }

    private UUID publicar(String tipo, String routingKey, Map<String, Object> datos) {
        SobreEvento sobre = EventosDePrueba.sobre(tipo, datos);
        rabbit.convertAndSend(eventos.exchange(), routingKey, sobre);
        esperarProcesado(sobre.eventoId());
        return sobre.eventoId();
    }

    private void publicarMismoEvento(SobreEvento sobre, String routingKey) {
        rabbit.convertAndSend(eventos.exchange(), routingKey, sobre);
    }

    private void esperarProcesado(UUID eventoId) {
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM evento_procesado WHERE evento_id = ?", Integer.class, eventoId)).isEqualTo(1));
    }

    private List<EnvioCorreo> enviosDe(UUID eventoId) {
        return envios.findAll().stream().filter(e -> eventoId.equals(e.getEventoId())).toList();
    }

    private EnvioCorreo unicoEnvio(UUID eventoId) {
        List<EnvioCorreo> encontrados = enviosDe(eventoId);
        assertThat(encontrados).hasSize(1);
        return encontrados.get(0);
    }

    private CorreoRenderizado render(EnvioCorreo envio) {
        var plantilla = plantillas.findById(envio.getPlantilla()).orElseThrow();
        return renderizador.renderizar(CatalogoPlantillas.valueOf(envio.getPlantilla()), TextosPlantilla.de(plantilla),
                envio.getVariables(), marca.actual());
    }

    private void crearPedido(String modalidad) {
        publicar("PedidoCreado", "order.pedido.creado", pedidoCreado(modalidad));
    }

    private void configurarAlertas() {
        publicar("ConfiguracionActualizada", "settings.configuracion.actualizada", Map.of(
                "version", 3, "secciones", List.of("PARAMETROS"),
                "configuracion", Map.of("identidad", Map.of("nombre", "FitStore"),
                        "tema", Map.of("colorPrimario", "#C9B28C", "colorTexto", "#26221D"),
                        "contenido", Map.of("email", "ventas@fitstore.test", "telefono", "987654321",
                                "horario", "Lunes a sábado de 9:00 a 19:00",
                                "tiendaFisica", Map.of("direccion", "Galería Gamarra, tienda 210"))),
                "correosAlertas", List.of(ALERTAS_1, ALERTAS_2)));
    }

    @Test
    @DisplayName("CA-7-01 los eventos al cliente crean su envío PENDIENTE con la plantilla y el destinatario que tocan")
    void eventosAlCliente() {
        crearPedido("DELIVERY_LOCAL");

        UUID registrado = publicar("ClienteRegistrado", "auth.cliente.registrado",
                Map.of("clienteId", UUID.randomUUID().toString(), "email", CLIENTE, "nombres", "Ana"));
        UUID recuperacion = publicar("PasswordResetSolicitado", "auth.password.reset-solicitado", Map.of(
                "cuentaId", UUID.randomUUID().toString(), "tipoCuenta", "CLIENTE", "email", CLIENTE, "nombres", "Ana",
                "motivo", "RECUPERACION", "enlace", "http://tienda.test/restablecer?token=abc",
                "expiraEn", "2026-10-22T15:30:00Z"));
        UUID invitacion = publicar("PasswordResetSolicitado", "auth.password.reset-solicitado", Map.of(
                "cuentaId", UUID.randomUUID().toString(), "tipoCuenta", "STAFF", "email", "operador@fitstore.test",
                "nombres", "Diana", "motivo", "INVITACION", "enlace", "http://admin.test/restablecer?token=xyz",
                "expiraEn", "2026-10-23T15:00:00Z"));
        UUID cambiado = publicar("PasswordCambiado", "auth.password.cambiado", Map.of(
                "cuentaId", UUID.randomUUID().toString(), "tipoCuenta", "CLIENTE", "email", CLIENTE, "nombres", "Ana"));
        UUID cancelado = publicar("PedidoCancelado", "order.pedido.cancelado", Map.of(
                "pedidoId", PEDIDO.toString(), "codigo", CODIGO, "fuePagado", true, "motivo", "Me equivoqué de talla",
                "actor", "CLIENTE", "items", List.of()));
        UUID listo = publicar("PedidoListoRecojo", "order.pedido.listo-recojo", Map.of(
                "pedidoId", PEDIDO.toString(), "codigo", CODIGO,
                "destinatario", Map.of("nombres", "Ana", "apellidos", "Quispe", "dni", "70123499",
                        "telefono", "987000099", "email", CLIENTE)));
        UUID recogido = publicar("PedidoRecogido", "order.pedido.recogido", Map.of(
                "pedidoId", PEDIDO.toString(), "codigo", CODIGO,
                "destinatario", Map.of("nombres", "Ana", "apellidos", "Quispe", "dni", "70123499",
                        "telefono", "987000099", "email", CLIENTE)));
        UUID fallida = publicar("EntregaActualizada", "delivery.entrega.actualizada",
                entrega("DELIVERY_LOCAL", "FALLIDA", Map.of("observacion", "Nadie en casa")));
        UUID enAgencia = publicar("EntregaActualizada", "delivery.entrega.actualizada",
                entrega("AGENCIA", "DISPONIBLE_EN_AGENCIA", Map.of("agenciaNombre", "Shalom", "claveRecojo", "K-4821")));
        UUID entregada = publicar("EntregaActualizada", "delivery.entrega.actualizada",
                entrega("DELIVERY_LOCAL", "ENTREGADA", Map.of()));

        assertEnvio(unicoEnvio(registrado), "BIENVENIDA", CLIENTE, null);
        assertEnvio(unicoEnvio(recuperacion), "RECUPERAR_PASSWORD", CLIENTE, null);
        assertEnvio(unicoEnvio(invitacion), "INVITACION_STAFF", "operador@fitstore.test", null);
        assertEnvio(unicoEnvio(cambiado), "PASSWORD_CAMBIADO", CLIENTE, null);
        assertEnvio(unicoEnvio(cancelado), "PEDIDO_CANCELADO", CLIENTE, CODIGO);
        assertEnvio(unicoEnvio(listo), "LISTO_PARA_RECOGER", CLIENTE, CODIGO);
        assertEnvio(unicoEnvio(recogido), "ENTREGADO", CLIENTE, CODIGO);
        assertEnvio(unicoEnvio(fallida), "ENTREGA_FALLIDA", CLIENTE, CODIGO);
        assertEnvio(unicoEnvio(enAgencia), "LISTO_PARA_RECOGER", CLIENTE, CODIGO);
        assertEnvio(unicoEnvio(entregada), "ENTREGADO", CLIENTE, CODIGO);
        assertThat(render(unicoEnvio(recuperacion)).html()).contains("http://tienda.test/restablecer?token=abc");
        assertThat(render(unicoEnvio(enAgencia)).html()).contains("Shalom").contains("K-4821");
    }

    @Test
    @DisplayName("CA-7-01 las alertas a la tienda van a cada correo para alertas de la copia de la configuración")
    void alertasALaTienda() {
        configurarAlertas();
        crearPedido("DELIVERY_LOCAL");

        UUID stockBajo = publicar("StockBajo", "catalog.stock.bajo", Map.of("varianteId", 10002, "sku", "LEG01-NEG-M",
                "producto", "Legging Power", "talla", "M", "color", "Negro", "disponible", 2, "minimo", 5));
        UUID sinStock = publicar("StockNoConfirmado", "catalog.stock.no-confirmado", Map.of("pedidoId", PEDIDO.toString(),
                "faltantes", List.of(Map.of("varianteId", 10002, "sku", "LEG01-NEG-M", "solicitado", 2, "disponible", 0))));
        UUID fallido = publicar("ReembolsoFallido", "payment.reembolso.fallido", Map.of("pedidoId", PEDIDO.toString(),
                "codigoPedido", CODIGO, "monto", new BigDecimal("299.70"), "ultimoError", "Timeout del proveedor",
                "intentos", 3));
        UUID reclamo = publicar("ReclamoRegistrado", "settings.reclamo.registrado", Map.of("numero", "RC-2026-000007",
                "tipo", "RECLAMO", "consumidor", persona("Ana Quispe", CLIENTE), "bien", Map.of("tipo", "PRODUCTO"),
                "detalle", "El producto llegó con un hilo suelto", "pedidoConsumidor", CODIGO,
                "fechaLimite", "2026-11-05", "creadoEn", "2026-10-22T15:00:00Z"));

        for (UUID evento : List.of(stockBajo, sinStock, fallido)) {
            assertThat(enviosDe(evento)).extracting(EnvioCorreo::getDestinatario)
                    .containsExactlyInAnyOrder(ALERTAS_1, ALERTAS_2);
            assertThat(enviosDe(evento)).allMatch(e -> e.getEstado() == EstadoEnvio.PENDIENTE);
        }
        assertThat(enviosDe(stockBajo)).extracting(EnvioCorreo::getPlantilla).containsOnly("ALERTA_STOCK_BAJO");
        assertThat(enviosDe(sinStock)).extracting(EnvioCorreo::getPlantilla).containsOnly("ALERTA_SIN_STOCK");
        assertThat(enviosDe(sinStock)).extracting(EnvioCorreo::getReferencia).containsOnly(CODIGO);
        assertThat(enviosDe(fallido)).extracting(EnvioCorreo::getPlantilla).containsOnly("ALERTA_REEMBOLSO_FALLIDO");
        // El reclamo genera la copia al consumidor (C12) y la alerta a cada correo de la tienda (T4)
        assertThat(enviosDe(reclamo)).extracting(EnvioCorreo::getPlantilla)
                .containsExactlyInAnyOrder("RECLAMO_COPIA", "ALERTA_RECLAMO_NUEVO", "ALERTA_RECLAMO_NUEVO");
        assertThat(enviosDe(reclamo)).extracting(EnvioCorreo::getDestinatario)
                .containsExactlyInAnyOrder(CLIENTE, ALERTAS_1, ALERTAS_2);
        assertThat(enviosDe(reclamo)).extracting(EnvioCorreo::getReferencia).containsOnly("RC-2026-000007");

        UUID respuesta = publicar("ReclamoRespondido", "settings.reclamo.respondido", Map.of("numero", "RC-2026-000007",
                "consumidor", persona("Ana Quispe", CLIENTE), "respuesta", "Cambiaremos tu producto.",
                "respondidoEn", "2026-10-23T10:00:00Z"));
        assertEnvio(unicoEnvio(respuesta), "RECLAMO_RESPUESTA", CLIENTE, "RC-2026-000007");
    }

    @Test
    @DisplayName("CA-7-01 BoletaEmitida crea la confirmación del pedido con la boleta adjunta")
    void confirmacionConBoleta() {
        crearPedido("DELIVERY_LOCAL");
        UUID boleta = publicar("BoletaEmitida", "payment.boleta.emitida",
                Map.of("pedidoId", PEDIDO.toString(), "numero", "B001-00000124", "url", "/api/v1/pagos/FS-000124/boleta"));

        EnvioCorreo envio = unicoEnvio(boleta);
        assertEnvio(envio, "PEDIDO_CONFIRMADO", CLIENTE, CODIGO);
        assertThat(envio.getAdjunto()).isEqualTo("B001-00000124");
    }

    @Test
    @DisplayName("CA-7-02 el mismo evento dos veces crea un solo envío")
    void eventoDuplicado() {
        SobreEvento sobre = EventosDePrueba.sobre("ClienteRegistrado",
                Map.of("clienteId", UUID.randomUUID().toString(), "email", CLIENTE, "nombres", "Ana"));
        publicarMismoEvento(sobre, "auth.cliente.registrado");
        esperarProcesado(sobre.eventoId());
        publicarMismoEvento(sobre, "auth.cliente.registrado");
        // Un evento distinto que llega después marca el final: el duplicado ya se consumió antes que él
        UUID siguiente = publicar("PasswordCambiado", "auth.password.cambiado", Map.of(
                "cuentaId", UUID.randomUUID().toString(), "tipoCuenta", "CLIENTE", "email", CLIENTE, "nombres", "Ana"));

        assertThat(enviosDe(sobre.eventoId())).hasSize(1);
        assertThat(enviosDe(siguiente)).hasSize(1);
    }

    @Test
    @DisplayName("CA-7-03 PedidoCreado, PedidoPagado y EntregaActualizada en PENDIENTE o ASIGNADA no envían correo")
    void eventosSinCorreo() {
        UUID creado = publicar("PedidoCreado", "order.pedido.creado", pedidoCreado("AGENCIA"));
        UUID pagado = publicar("PedidoPagado", "order.pedido.pagado",
                Map.of("pedidoId", PEDIDO.toString(), "codigo", CODIGO, "items", List.of()));
        UUID pendiente = publicar("EntregaActualizada", "delivery.entrega.actualizada",
                entrega("DELIVERY_LOCAL", "PENDIENTE_ASIGNACION", Map.of()));
        UUID asignada = publicar("EntregaActualizada", "delivery.entrega.actualizada",
                entrega("DELIVERY_LOCAL", "ASIGNADA", Map.of("fechaProgramada", "2026-10-24")));
        UUID envioPendiente = publicar("EntregaActualizada", "delivery.entrega.actualizada",
                entrega("AGENCIA", "PENDIENTE_ENVIO", Map.of()));

        for (UUID evento : List.of(creado, pagado, pendiente, asignada, envioPendiente)) {
            assertThat(enviosDe(evento)).isEmpty();
        }
        assertThat(envios.count()).isZero();
        // PedidoCreado guarda la copia del pedido (sin DNI) y PedidoPagado inicia el respaldo de la boleta
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pedido_resumen WHERE codigo = ?", Integer.class, CODIGO))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT pagado_en IS NOT NULL FROM pedido_resumen WHERE codigo = ?",
                Boolean.class, CODIGO)).isTrue();
        assertThat(jdbc.queryForObject("SELECT items::text || totales::text FROM pedido_resumen WHERE codigo = ?",
                String.class, CODIGO)).doesNotContain("70123499");
        // PedidoExpirado y PagoRechazado ni siquiera se escuchan: la cola no se enlaza a sus routing keys
        assertThat(eventos.enlaces()).doesNotContain("order.pedido.expirado", "payment.pago.rechazado");
    }

    @Test
    @DisplayName("CA-7-04 una plantilla desactivada no genera envío")
    void plantillaDesactivada() {
        jdbc.update("UPDATE plantilla SET activa = false WHERE codigo = 'BIENVENIDA'");

        UUID registrado = publicar("ClienteRegistrado", "auth.cliente.registrado",
                Map.of("clienteId", UUID.randomUUID().toString(), "email", CLIENTE, "nombres", "Ana"));
        UUID cambiado = publicar("PasswordCambiado", "auth.password.cambiado", Map.of(
                "cuentaId", UUID.randomUUID().toString(), "tipoCuenta", "CLIENTE", "email", CLIENTE, "nombres", "Ana"));

        assertThat(enviosDe(registrado)).isEmpty();
        assertThat(enviosDe(cambiado)).hasSize(1);
    }

    @Test
    @DisplayName("CA-7-05 en camino por agencia lleva agencia, seguimiento y enlace; en delivery local, fecha y repartidor")
    void enCamino() {
        crearPedido("AGENCIA");
        UUID agencia = publicar("EntregaActualizada", "delivery.entrega.actualizada", entrega("AGENCIA", "ENVIADO_AGENCIA",
                Map.of("agenciaNombre", "Shalom", "codigoSeguimiento", "SH-2026-48121",
                        "urlSeguimiento", "https://rastreo.shalom.test/SH-2026-48121")));
        UUID local = publicar("EntregaActualizada", "delivery.entrega.actualizada", entrega("DELIVERY_LOCAL", "EN_RUTA",
                Map.of("fechaProgramada", "2026-10-22", "repartidorNombre", "Julio Rojas",
                        "repartidorTelefono", "987111222")));

        EnvioCorreo porAgencia = unicoEnvio(agencia);
        assertEnvio(porAgencia, "EN_CAMINO", CLIENTE, CODIGO);
        assertThat(render(porAgencia).html()).contains("Shalom").contains("SH-2026-48121")
                .contains("https://rastreo.shalom.test/SH-2026-48121");
        EnvioCorreo enLocal = unicoEnvio(local);
        assertEnvio(enLocal, "EN_CAMINO", CLIENTE, CODIGO);
        assertThat(render(enLocal).html()).contains("jueves 22 de octubre").contains("Julio Rojas")
                .doesNotContain("987111222");
    }

    @Test
    @DisplayName("CA-7-06 PagoReembolsado parcial o manual dice 'reembolso parcial de tu pedido' con monto y motivo")
    void reembolsoParcial() {
        UUID parcial = publicar("PagoReembolsado", "payment.reembolso.completado", Map.of("pedidoId", PEDIDO.toString(),
                "codigoPedido", CODIGO, "solicitudId", UUID.randomUUID().toString(), "monto", new BigDecimal("79.90"),
                "parcial", true, "motivo", "POSTVENTA", "manual", false, "cliente", persona("Ana Quispe", CLIENTE)));
        UUID total = publicar("PagoReembolsado", "payment.reembolso.completado", Map.of("pedidoId", PEDIDO.toString(),
                "codigoPedido", CODIGO, "solicitudId", UUID.randomUUID().toString(), "monto", new BigDecimal("299.70"),
                "parcial", false, "motivo", "CANCELACION", "manual", false, "cliente", persona("Ana Quispe", CLIENTE)));

        EnvioCorreo envioParcial = unicoEnvio(parcial);
        assertEnvio(envioParcial, "REEMBOLSO_REALIZADO", CLIENTE, CODIGO);
        assertThat(render(envioParcial).html().toLowerCase()).contains("reembolso parcial de tu pedido")
                .contains("S/ 79.90".toLowerCase());
        assertThat(render(envioParcial).texto()).containsIgnoringCase("postventa");
        EnvioCorreo envioTotal = unicoEnvio(total);
        assertThat(render(envioTotal).html().toLowerCase()).doesNotContain("reembolso parcial").contains("s/ 299.70");
    }

    @Test
    @DisplayName("CA-7-07 EntregaActualizada DEVUELTA envía 'Tu pedido regresó a la tienda' con motivo y contacto de la tienda")
    void pedidoDevuelto() {
        crearPedido("AGENCIA");
        UUID devuelta = publicar("EntregaActualizada", "delivery.entrega.actualizada", entrega("AGENCIA", "DEVUELTA",
                Map.of("observacion", "La agencia no pudo entregarlo")));

        EnvioCorreo envio = unicoEnvio(devuelta);
        assertEnvio(envio, "PEDIDO_DEVUELTO", CLIENTE, CODIGO);
        CorreoRenderizado correo = render(envio);
        assertThat(correo.asunto()).contains("regresó a la tienda");
        assertThat(correo.html()).contains("La agencia no pudo entregarlo").contains("ventas@tienda.pe")
                .contains("987654321");
    }

    @Test
    @DisplayName("CA-7-10 con el SMTP caído los eventos se siguen consumiendo y guardando")
    void smtpCaidoNoFrenaLaCola() {
        // No hay servidor SMTP en esta prueba (puerto 3025 cerrado): el consumo no depende de él
        List<UUID> eventosPublicados = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            eventosPublicados.add(publicar("ClienteRegistrado", "auth.cliente.registrado",
                    Map.of("clienteId", UUID.randomUUID().toString(), "email", "cliente" + i + "@fitstore.test",
                            "nombres", "Cliente " + i)));
        }

        assertThat(envios.findAll()).hasSize(5).allMatch(e -> e.getEstado() == EstadoEnvio.PENDIENTE);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(
                rabbit.execute(canal -> canal.messageCount(eventos.cola()))).isZero());
    }

    private static void assertEnvio(EnvioCorreo envio, String plantilla, String destinatario, String referencia) {
        assertThat(envio.getPlantilla()).isEqualTo(plantilla);
        assertThat(envio.getDestinatario()).isEqualTo(destinatario);
        assertThat(envio.getEstado()).isEqualTo(EstadoEnvio.PENDIENTE);
        assertThat(envio.getReferencia()).isEqualTo(referencia);
        assertThat(envio.getAsunto()).isNotBlank().doesNotContain("{{");
    }
}
