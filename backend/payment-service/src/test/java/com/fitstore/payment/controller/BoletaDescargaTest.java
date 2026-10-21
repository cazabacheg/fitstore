package com.fitstore.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.payment.model.Boleta;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.pruebas.BoletaApiBase;
import com.fitstore.payment.pruebas.GaragePrueba;
import com.fitstore.payment.pruebas.Sesiones;
import com.fitstore.payment.service.BoletaService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** Descarga de la boleta: solo la dueña, sin URL pública del archivo (CA-5-29) e interno para notification. */
class BoletaDescargaTest extends BoletaApiBase {

    @Autowired BoletaService boletas;

    private final UUID clienta = UUID.randomUUID();

    @Test
    @DisplayName("CA-5-29 la dueña descarga su boleta en PDF con el nombre del número")
    void descargaLaDuena() throws Exception {
        IntencionPago i = intencionPagada(clienta);
        Boleta boleta = boletas.emitir(i.getId());

        MvcResult resultado = mvc.perform(get("/api/v1/pagos/" + i.getCodigoPedido() + "/boleta").with(Sesiones.cliente(clienta)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"Boleta-" + boleta.getNumero() + ".pdf\""))
                .andReturn();

        byte[] pdf = resultado.getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        assertThat(textoDelPdf(pdf)).contains(boleta.getNumero());
    }

    @Test
    @DisplayName("CA-5-29 la boleta de otra clienta responde 404")
    void ajena() throws Exception {
        IntencionPago i = intencionPagada(clienta);
        boletas.emitir(i.getId());

        mvc.perform(get("/api/v1/pagos/" + i.getCodigoPedido() + "/boleta").with(Sesiones.cliente(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("CA-5-29 mientras la boleta no se emite responde 404")
    void sinBoletaTodavia() throws Exception {
        IntencionPago i = intencionPagada(clienta);

        mvc.perform(get("/api/v1/pagos/" + i.getCodigoPedido() + "/boleta").with(Sesiones.cliente(clienta)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-5-29 el archivo no tiene URL pública: sin credenciales Garage niega el acceso al bucket privado")
    void sinUrlPublica() throws Exception {
        IntencionPago i = intencionPagada(clienta);
        Boleta boleta = boletas.emitir(i.getId());

        HttpResponse<String> anonimo = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(GaragePrueba.endpoint() + "/" + GaragePrueba.BUCKET_PRIVADO + "/" + boleta.getClave()))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(anonimo.statusCode()).isIn(401, 403);
    }

    @Test
    @DisplayName("CA-5-29 el endpoint interno entrega el PDF por su número a notification, y un número desconocido da 404")
    void internoPorNumero() throws Exception {
        IntencionPago i = intencionPagada(clienta);
        Boleta boleta = boletas.emitir(i.getId());

        byte[] pdf = mvc.perform(get("/internal/v1/boletas/" + boleta.getNumero()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

        mvc.perform(get("/internal/v1/boletas/B001-99999999")).andExpect(status().isNotFound());
    }
}
