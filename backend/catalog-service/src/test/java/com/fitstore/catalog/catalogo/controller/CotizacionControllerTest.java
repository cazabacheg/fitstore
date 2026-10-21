package com.fitstore.catalog.catalogo.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.pruebas.ApiBase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** Cotización pública para el carrito y el checkout (HU-2-03). */
class CotizacionControllerTest extends ApiBase {

    private ResultActions cotizar(String lineasJson) throws Exception {
        return mvc.perform(post("/api/v1/precios/cotizar").contentType(APPLICATION_JSON)
                .content("{\"lineas\":[" + lineasJson + "]}"));
    }

    private static String l(Long varianteId, int cantidad) {
        return "{\"varianteId\":" + varianteId + ",\"cantidad\":" + cantidad + "}";
    }

    @Test
    @DisplayName("CA-2-13 cotiza el carrito del ejemplo: el legging lleva 10 % (−36.94) y el top no lleva descuento")
    void ejemploDelMicro() throws Exception {
        Categoria c = categoria("Cotización", null);
        Producto legging = producto("COT13L", "Legging Pro", "59.90", EstadoProducto.ACTIVO, c);
        Variante negroM = variante(legging, "M", "negro", 10, 0);
        Variante azulS = variante(legging, "S", "azul", 10, 0);
        Variante negroL = variante(legging, "L", "negro", 10, 0);
        negroL.setPrecio(dec("64.90"));
        variantes.saveAndFlush(negroL);
        Producto top = producto("COT13T", "Top Flex", "39.90", EstadoProducto.ACTIVO, c);
        Variante rojoS = variante(top, "S", "rojo", 10, 0);
        sincronizar();

        cotizar(l(negroM.getId(), 2) + "," + l(azulS.getId(), 2) + "," + l(negroL.getId(), 2) + "," + l(rojoS.getId(), 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lineas", hasSize(4)))
                .andExpect(jsonPath("$.data.lineas[0].estado").value("OK"))
                .andExpect(jsonPath("$.data.lineas[0].producto").value("Legging Pro"))
                .andExpect(jsonPath("$.data.lineas[0].talla").value("M"))
                .andExpect(jsonPath("$.data.lineas[0].color").value("negro"))
                .andExpect(jsonPath("$.data.lineas[0].precioUnitario").value(59.9))
                .andExpect(jsonPath("$.data.lineas[0].subtotal").value(119.8))
                .andExpect(jsonPath("$.data.lineas[0].descuentoPorcentaje").value(10))
                .andExpect(jsonPath("$.data.lineas[0].descuentoMonto").value(11.98))
                .andExpect(jsonPath("$.data.lineas[2].precioUnitario").value(64.9))
                .andExpect(jsonPath("$.data.lineas[2].descuentoMonto").value(12.98))
                .andExpect(jsonPath("$.data.lineas[3].descuentoMonto").value(0))
                .andExpect(jsonPath("$.data.lineas[3].total").value(39.9))
                .andExpect(jsonPath("$.data.subtotal").value(409.3))
                .andExpect(jsonPath("$.data.descuentoTotal").value(36.94))
                .andExpect(jsonPath("$.data.total").value(372.36))
                .andExpect(jsonPath("$.data.lineas[0].disponible", nullValue()));
    }

    @Test
    @DisplayName("CA-2-14 indica cuánto falta para el siguiente tramo")
    void cuantoFalta() throws Exception {
        Categoria c = categoria("Falta", null);
        Producto p = producto("COT14A", "Legging Falta", "50.00", EstadoProducto.ACTIVO, c);
        Variante v = variante(p, "M", "negro", 20, 0);
        sincronizar();

        cotizar(l(v.getId(), 4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productos[0].unidades").value(4))
                .andExpect(jsonPath("$.data.productos[0].tramoAlcanzado.unidadesMinimas").value(3))
                .andExpect(jsonPath("$.data.productos[0].tramoAlcanzado.porcentaje").value(5))
                .andExpect(jsonPath("$.data.productos[0].siguienteTramo.unidadesMinimas").value(6))
                .andExpect(jsonPath("$.data.productos[0].siguienteTramo.porcentaje").value(10))
                .andExpect(jsonPath("$.data.productos[0].siguienteTramo.faltan").value(2));
    }

    @Test
    @DisplayName("CA-2-15 una variante inactiva, un producto no ACTIVO o una variante inexistente vuelven NO_DISPONIBLE sin romper las demás")
    void lineasNoDisponibles() throws Exception {
        Categoria c = categoria("No disponible", null);
        Producto ok = producto("COT15A", "Producto Bueno", "50.00", EstadoProducto.ACTIVO, c);
        Variante buena = variante(ok, "M", "negro", 5, 0);
        Variante inactiva = variante(ok, "S", "negro", 5, 0);
        inactiva.setActivo(false);
        variantes.saveAndFlush(inactiva);
        Producto borrador = producto("COT15B", "Producto Borrador", "50.00", EstadoProducto.BORRADOR, c);
        Variante deBorrador = variante(borrador, "M", "negro", 5, 0);
        sincronizar();

        cotizar(l(buena.getId(), 1) + "," + l(inactiva.getId(), 1) + "," + l(deBorrador.getId(), 1) + "," + l(999999999L, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lineas[0].estado").value("OK"))
                .andExpect(jsonPath("$.data.lineas[1].estado").value("NO_DISPONIBLE"))
                .andExpect(jsonPath("$.data.lineas[1].total").value(0))
                .andExpect(jsonPath("$.data.lineas[2].estado").value("NO_DISPONIBLE"))
                .andExpect(jsonPath("$.data.lineas[3].estado").value("NO_DISPONIBLE"))
                .andExpect(jsonPath("$.data.lineas[3].varianteId").value(999999999L))
                .andExpect(jsonPath("$.data.total").value(50.0));
    }

    @Test
    @DisplayName("CA-2-13 la línea que pide más de lo disponible vuelve STOCK_INSUFICIENTE con cuántas quedan, y las que alcanzan no traen el stock")
    void stockInsuficiente() throws Exception {
        Categoria c = categoria("Stock", null);
        Producto p = producto("COT13S", "Legging Stock", "50.00", EstadoProducto.ACTIVO, c);
        Variante poca = variante(p, "M", "negro", 3, 1);
        Variante mucha = variante(p, "S", "negro", 50, 0);
        sincronizar();

        cotizar(l(poca.getId(), 3) + "," + l(mucha.getId(), 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lineas[0].estado").value("STOCK_INSUFICIENTE"))
                .andExpect(jsonPath("$.data.lineas[0].disponible").value(2))
                .andExpect(jsonPath("$.data.lineas[1].estado").value("OK"))
                .andExpect(jsonPath("$.data.lineas[1].disponible", nullValue()));
    }

    @Test
    @DisplayName("CA-2-16 más de 50 líneas responde 400")
    void masDeCincuentaLineas() throws Exception {
        List<String> lineas = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            lineas.add(l(1L + i, 1));
        }

        cotizar(String.join(",", lineas))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
        cotizar("").andExpect(status().isBadRequest());
    }
}
