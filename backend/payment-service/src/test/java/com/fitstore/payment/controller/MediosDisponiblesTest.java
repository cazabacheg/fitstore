package com.fitstore.payment.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.provider.PaymentProvider;
import com.fitstore.payment.provider.ResultadoCargo;
import com.fitstore.payment.provider.SolicitudCargo;
import com.fitstore.payment.pruebas.PagoApiBase;
import com.fitstore.payment.pruebas.Sesiones;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

/**
 * Reglas del listado de medios (P6, P7) con un proveedor de prueba que ofrece Yape: la tienda no lo habilita en esta
 * entrega, pero la regla del tope vive en el listado y se verifica aquí.
 */
@Import(MediosDisponiblesTest.ConYape.class)
class MediosDisponiblesTest extends PagoApiBase {

    /** Proveedor de prueba con credenciales que ofrece tarjeta y Yape (no cobra nada). */
    @TestConfiguration
    static class ConYape {
        @Bean
        PaymentProvider proveedorConYape() {
            return new PaymentProvider() {
                @Override public Proveedor proveedor() { return Proveedor.CULQI; }
                @Override public boolean tieneCredenciales() { return true; }
                @Override public String clavePublica() { return "pk_test_culqi"; }
                @Override public List<Medio> medios() { return List.of(Medio.TARJETA, Medio.YAPE); }
                @Override public ResultadoCargo crearCargo(SolicitudCargo solicitud) { throw new UnsupportedOperationException(); }
                @Override public ResultadoCargo consultarEstado(String idProveedor) { throw new UnsupportedOperationException(); }
            };
        }
    }

    private final UUID clienta = UUID.randomUUID();

    @Test
    @DisplayName("CA-5-05 con un total mayor a S/ 500 Yape aparece deshabilitado con el aviso del tope")
    void yapeDeshabilitadoSobreElTope() throws Exception {
        when(tienda.metodosPago()).thenReturn(Optional.of(List.of("CULQI", "YAPE")));
        IntencionPago i = intencion(clienta, "650.00", java.time.Instant.now().plusSeconds(1800));

        mvc.perform(get("/api/v1/pagos/" + i.getCodigoPedido()).with(Sesiones.cliente(clienta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topeYape").value(500.00))
                .andExpect(jsonPath("$.data.medios[?(@.medio=='YAPE')].disponible").value(hasItem(false)))
                .andExpect(jsonPath("$.data.medios[?(@.medio=='YAPE')].motivoNoDisponible")
                        .value(hasItem("Yape permite hasta S/ 500 por día")))
                .andExpect(jsonPath("$.data.medios[?(@.medio=='TARJETA' && @.proveedor=='CULQI')].disponible")
                        .value(hasItem(true)));
    }

    @Test
    @DisplayName("CA-5-05 con un total de hasta S/ 500 Yape queda disponible")
    void yapeDisponibleDentroDelTope() throws Exception {
        when(tienda.metodosPago()).thenReturn(Optional.of(List.of("CULQI", "YAPE")));
        IntencionPago i = intencion(clienta, "500.00", java.time.Instant.now().plusSeconds(1800));

        mvc.perform(get("/api/v1/pagos/" + i.getCodigoPedido()).with(Sesiones.cliente(clienta)))
                .andExpect(jsonPath("$.data.medios[?(@.medio=='YAPE')].disponible").value(hasItem(true)));
    }

    @Test
    @DisplayName("CA-5-05 el backend también rechaza un cobro con Yape sobre el tope")
    void elBackendValidaElTope() throws Exception {
        when(tienda.metodosPago()).thenReturn(Optional.of(List.of("CULQI", "YAPE")));
        IntencionPago i = intencion(clienta, "650.00", java.time.Instant.now().plusSeconds(1800));

        mvc.perform(post("/api/v1/pagos/" + i.getCodigoPedido() + "/cargos").with(Sesiones.cliente(clienta))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proveedor\":\"CULQI\",\"medio\":\"YAPE\",\"token\":\"tkn_test_123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Yape permite hasta S/ 500 por día"));
    }
}
