package com.fitstore.catalog.catalogo.service;

import static com.fitstore.catalog.pruebas.Sesiones.admin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.catalog.pruebas.ApiBase;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

/** La validación de la carga masiva vence (30 minutos en producción; aquí 1 segundo para poder probarlo). */
@TestPropertySource(properties = "fitstore.catalogo.carga-masiva.vigencia=PT1S")
class CargaMasivaVencimientoTest extends ApiBase {

    @Test
    @DisplayName("CA-2-61 una validación de hace más de 30 minutos no se puede confirmar y se indica el motivo")
    void validacionVencida() throws Exception {
        categoria("Vencimiento", null);
        sincronizar();
        String csv = "codigo,nombre,descripcion,precio_base,categorias,talla,color,precio_variante,stock_inicial\n"
                + "VENC1,Producto vencido,Desc,50,Vencimiento,S,negro,,1\n";
        String id = cuerpo(mvc.perform(multipart("/api/v1/admin/productos/carga-masiva/validar")
                        .file(new MockMultipartFile("archivo", "v.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8))).with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.valida").value(true))).path("data").path("validacionId").asText();

        Thread.sleep(1500);

        mvc.perform(post("/api/v1/admin/productos/carga-masiva/" + id + "/confirmar").with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_VENCIDA"))
                .andExpect(jsonPath("$.mensaje").value(containsString("30 minutos")));
        assertThat(productos.findByCodigo("VENC1")).isEmpty();
    }
}
