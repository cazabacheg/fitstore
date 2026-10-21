package com.fitstore.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.EstadoReembolso;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.MotivoReembolso;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.model.Reembolso;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.pruebas.BoletaApiBase;
import com.fitstore.payment.pruebas.Datos;
import com.fitstore.payment.pruebas.Sesiones;
import com.fitstore.payment.repository.ReembolsoRepository;
import com.fitstore.payment.service.BoletaService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Backoffice de pagos (solo ADMIN): listado con filtros, detalle y boleta (HU-5-11: CA-5-33 a CA-5-35). */
class AdminPagoControllerTest extends BoletaApiBase {

    @Autowired ReembolsoRepository reembolsos;
    @Autowired BoletaService boletas;

    private final UUID clienta = UUID.randomUUID();

    @Test
    @DisplayName("CA-5-33 el listado muestra código, cliente, monto, proveedor y medio del cobro, estado, fecha e intentos")
    void listaConSusColumnas() throws Exception {
        IntencionPago i = intencionPagada(clienta);

        mvc.perform(get("/api/v1/admin/pagos").param("codigo", i.getCodigoPedido()).with(Sesiones.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.contenido", hasSize(1)))
                .andExpect(jsonPath("$.data.contenido[0].id").value(i.getId().toString()))
                .andExpect(jsonPath("$.data.contenido[0].codigoPedido").value(i.getCodigoPedido()))
                .andExpect(jsonPath("$.data.contenido[0].clienteNombre").value("Ana Quispe Rojas"))
                .andExpect(jsonPath("$.data.contenido[0].monto").value(129.80))
                .andExpect(jsonPath("$.data.contenido[0].proveedor").value("MERCADO_PAGO"))
                .andExpect(jsonPath("$.data.contenido[0].medio").value("TARJETA"))
                .andExpect(jsonPath("$.data.contenido[0].intentos").value(1))
                .andExpect(jsonPath("$.data.contenido[0].estado").value("PAGADA"))
                .andExpect(jsonPath("$.data.contenido[0].creadoEn").exists())
                .andExpect(jsonPath("$.data.contenido[0].tieneReembolsoFallido").value(false))
                .andExpect(jsonPath("$.data.pagina").value(0))
                .andExpect(jsonPath("$.data.totalElementos").value(1));
    }

    @Test
    @DisplayName("CA-5-33 se pagina con page y size")
    void pagina() throws Exception {
        intencion(clienta);
        intencion(clienta);
        intencion(clienta);

        mvc.perform(get("/api/v1/admin/pagos").param("page", "0").param("size", "2").with(Sesiones.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido", hasSize(2)))
                .andExpect(jsonPath("$.data.tamano").value(2))
                .andExpect(jsonPath("$.data.totalElementos", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.totalPaginas", greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("CA-5-33 filtra por estado")
    void filtraPorEstado() throws Exception {
        IntencionPago pendiente = intencion(clienta);
        IntencionPago anulada = intencion(clienta);
        anulada.setEstado(EstadoIntencion.ANULADA);
        intenciones.saveAndFlush(anulada);

        mvc.perform(get("/api/v1/admin/pagos").param("estado", "ANULADA").param("codigo", "FS-").with(Sesiones.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido[?(@.id=='" + anulada.getId() + "')]").isNotEmpty())
                .andExpect(jsonPath("$.data.contenido[?(@.id=='" + pendiente.getId() + "')]").isEmpty());
    }

    @Test
    @DisplayName("CA-5-33 filtra por proveedor de cualquiera de sus intentos")
    void filtraPorProveedor() throws Exception {
        IntencionPago conMercadoPago = intencionPagada(clienta);
        IntencionPago conSimulado = intencion(clienta);
        transaccionProcesando(conSimulado, Proveedor.SIMULADO, "sim-1", 0);

        mvc.perform(get("/api/v1/admin/pagos").param("proveedor", "SIMULADO").with(Sesiones.admin()).param("size", "100"))
                .andExpect(jsonPath("$.data.contenido[?(@.id=='" + conSimulado.getId() + "')]").isNotEmpty())
                .andExpect(jsonPath("$.data.contenido[?(@.id=='" + conMercadoPago.getId() + "')]").isEmpty());
    }

    @Test
    @DisplayName("CA-5-33 filtra por fechas (día de Lima, ambos extremos incluidos)")
    void filtraPorFechas() throws Exception {
        IntencionPago i = intencion(clienta);
        // 15/01/2026 12:00 hora de Lima
        jdbc.update("UPDATE intencion_pago SET creado_en = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2026-01-15T17:00:00Z")), i.getId());

        mvc.perform(get("/api/v1/admin/pagos").param("desde", "2026-01-15").param("hasta", "2026-01-15")
                        .param("codigo", i.getCodigoPedido()).with(Sesiones.admin()))
                .andExpect(jsonPath("$.data.contenido", hasSize(1)));
        mvc.perform(get("/api/v1/admin/pagos").param("desde", "2026-01-16").param("codigo", i.getCodigoPedido()).with(Sesiones.admin()))
                .andExpect(jsonPath("$.data.contenido", hasSize(0)));
        mvc.perform(get("/api/v1/admin/pagos").param("hasta", "2026-01-14").param("codigo", i.getCodigoPedido()).with(Sesiones.admin()))
                .andExpect(jsonPath("$.data.contenido", hasSize(0)));
    }

    @Test
    @DisplayName("CA-5-33 filtra por código de pedido sin distinguir mayúsculas")
    void filtraPorCodigo() throws Exception {
        IntencionPago buscada = intencion(clienta);
        intencion(clienta);

        mvc.perform(get("/api/v1/admin/pagos").param("codigo", buscada.getCodigoPedido().toLowerCase()).with(Sesiones.admin()))
                .andExpect(jsonPath("$.data.contenido", hasSize(1)))
                .andExpect(jsonPath("$.data.contenido[0].codigoPedido").value(buscada.getCodigoPedido()));
    }

    @Test
    @DisplayName("CA-5-33 ordena por un campo permitido y rechaza uno desconocido")
    void ordena() throws Exception {
        mvc.perform(get("/api/v1/admin/pagos").param("sort", "monto,desc").with(Sesiones.admin()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/pagos").param("sort", "cliente,desc").with(Sesiones.admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-5-34 el detalle muestra intentos, reembolsos, lo cobrado, lo reembolsado y lo neto, la boleta y el pedido, sin datos de tarjeta")
    void detalle() throws Exception {
        IntencionPago i = intencionPagada(clienta);
        Transaccion cobro = transacciones.findByIntencionIdOrderByCreadoEnAsc(i.getId()).get(0);
        Reembolso parcial = Datos.reembolso(i.getId(), cobro.getId(), MotivoReembolso.POSTVENTA);
        parcial.setMonto(new BigDecimal("50.00"));
        parcial.setEstado(EstadoReembolso.COMPLETADO);
        parcial.setSolicitudId(UUID.randomUUID());
        reembolsos.saveAndFlush(parcial);
        var boleta = boletas.emitir(i.getId());

        mvc.perform(get("/api/v1/admin/pagos/" + i.getId()).with(Sesiones.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(i.getId().toString()))
                .andExpect(jsonPath("$.data.pedidoId").value(i.getPedidoId().toString()))
                .andExpect(jsonPath("$.data.codigoPedido").value(i.getCodigoPedido()))
                .andExpect(jsonPath("$.data.cliente.nombre").value("Ana Quispe Rojas"))
                .andExpect(jsonPath("$.data.cliente.dni").value("12345678"))
                .andExpect(jsonPath("$.data.cliente.email").value("ana@correo.pe"))
                .andExpect(jsonPath("$.data.transacciones", hasSize(1)))
                .andExpect(jsonPath("$.data.transacciones[0].proveedor").value("MERCADO_PAGO"))
                .andExpect(jsonPath("$.data.transacciones[0].medio").value("TARJETA"))
                .andExpect(jsonPath("$.data.transacciones[0].estado").value("APROBADA"))
                .andExpect(jsonPath("$.data.transacciones[0].monto").value(129.80))
                .andExpect(jsonPath("$.data.reembolsos", hasSize(1)))
                .andExpect(jsonPath("$.data.reembolsos[0].motivo").value("POSTVENTA"))
                .andExpect(jsonPath("$.data.reembolsos[0].monto").value(50.00))
                .andExpect(jsonPath("$.data.reembolsos[0].estado").value("COMPLETADO"))
                .andExpect(jsonPath("$.data.montoReembolsado").value(50.00))
                .andExpect(jsonPath("$.data.neto").value(79.80))
                .andExpect(jsonPath("$.data.boleta.numero").value(boleta.getNumero()))
                .andExpect(jsonPath("$.data.boleta.emitidaEn").exists())
                .andExpect(jsonPath("$.data.respuesta").doesNotExist())
                .andExpect(jsonPath("$..token").doesNotExist());
    }

    @Test
    @DisplayName("CA-5-34 un pago sin cobrar ni boleta muestra boleta nula, reembolsado cero y neto cero")
    void detalleSinCobrar() throws Exception {
        IntencionPago i = intencion(clienta);

        mvc.perform(get("/api/v1/admin/pagos/" + i.getId()).with(Sesiones.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.data.boleta").doesNotExist())
                .andExpect(jsonPath("$.data.montoReembolsado").value(0))
                .andExpect(jsonPath("$.data.neto").value(0));
    }

    @Test
    @DisplayName("CA-5-34 un pago inexistente responde 404")
    void detalleInexistente() throws Exception {
        mvc.perform(get("/api/v1/admin/pagos/" + UUID.randomUUID()).with(Sesiones.admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-5-34 el admin descarga la boleta de un pago y, sin boleta, recibe 404")
    void descargaLaBoleta() throws Exception {
        IntencionPago conBoleta = intencionPagada(clienta);
        var boleta = boletas.emitir(conBoleta.getId());
        IntencionPago sinBoleta = intencionPagada(clienta);

        byte[] pdf = mvc.perform(get("/api/v1/admin/pagos/" + conBoleta.getId() + "/boleta").with(Sesiones.admin()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(textoDelPdf(pdf)).contains(boleta.getNumero());
        mvc.perform(get("/api/v1/admin/pagos/" + sinBoleta.getId() + "/boleta").with(Sesiones.admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-5-35 un OPERADOR recibe 403 en el listado, el detalle y la boleta")
    void operadorNoEntra() throws Exception {
        IntencionPago i = intencionPagada(clienta);

        mvc.perform(get("/api/v1/admin/pagos").with(Sesiones.operador())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/pagos/" + i.getId()).with(Sesiones.operador())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/pagos/" + i.getId() + "/boleta").with(Sesiones.operador()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    @Test
    @DisplayName("CA-5-35 sin sesión responde 401 y una clienta recibe 403")
    void sinSesionOClienta() throws Exception {
        mvc.perform(get("/api/v1/admin/pagos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/pagos").with(Sesiones.cliente(clienta))).andExpect(status().isForbidden());
    }
}
