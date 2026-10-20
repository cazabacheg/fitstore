package com.fitstore.catalog.catalogo.service;

import static com.fitstore.catalog.pruebas.Sesiones.ADMIN_ID;
import static com.fitstore.catalog.pruebas.Sesiones.admin;
import static com.fitstore.catalog.pruebas.Sesiones.operador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.pruebas.ApiBase;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

/** Carga masiva de productos por CSV o XLSX: plantilla, validar sin crear nada y confirmar todo o nada (HU-2-12). */
class CargaMasivaServiceTest extends ApiBase {

    private static final String CARGA = "/api/v1/admin/productos/carga-masiva";
    private static final String CABECERA = "codigo,nombre,descripcion,precio_base,categorias,talla,color,precio_variante,stock_inicial";

    private Categoria leggings;
    private Categoria tops;

    private void categorias() {
        leggings = categoria("Cargaleggings", null);
        tops = categoria("Cargatops", null);
        sincronizar();
    }

    private static MockMultipartFile csv(String... filas) {
        String contenido = CABECERA + "\n" + String.join("\n", filas) + "\n";
        return new MockMultipartFile("archivo", "productos.csv", "text/csv", contenido.getBytes(StandardCharsets.UTF_8));
    }

    private ResultActions validar(MockMultipartFile archivo) throws Exception {
        return mvc.perform(multipart(CARGA + "/validar").file(archivo).with(admin()));
    }

    private ResultActions confirmar(String validacionId) throws Exception {
        return mvc.perform(post(CARGA + "/" + validacionId + "/confirmar").with(admin()));
    }

    private String idValidacion(ResultActions r) throws Exception {
        return cuerpo(r).path("data").path("validacionId").asText();
    }

    private int productosConPrefijo(String prefijo) {
        sincronizar();
        return jdbc.queryForObject("SELECT count(*) FROM producto WHERE codigo LIKE ?", Integer.class, prefijo + "%");
    }

    private String filasBuenas() {
        return "CARGA1,Legging Carga,Tela de compresión,59.90,Cargaleggings,S,negro,,10\n"
                + "CARGA1,Legging Carga,Tela de compresión,59.90,Cargaleggings,M,negro,64.90,5\n"
                + "CARGA1,Legging Carga,Tela de compresión,59.90,Cargaleggings,M,rojo,,0\n"
                + "CARGA2,Top Carga,Top deportivo,39.90,Cargatops|Cargaleggings,S,blanco,,3\n"
                + "CARGA2,Top Carga,Top deportivo,39.90,Cargatops|Cargaleggings,M,blanco,,";
    }

    // ------------------------------------------------------------ CA-2-58

    @Test
    @DisplayName("CA-2-58 la plantilla CSV trae las columnas de la carga masiva, una fila por variante")
    void plantillaCsv() throws Exception {
        String texto = mvc.perform(get(CARGA + "/plantilla").param("formato", "csv").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Disposition", containsString("plantilla-productos.csv")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        List<String> lineas = texto.lines().toList();
        assertThat(lineas.get(0).replace("﻿", "")).isEqualTo(CABECERA);
        assertThat(lineas).hasSizeGreaterThanOrEqualTo(2);
        assertThat(lineas.get(1).split(",", -1)).hasSize(9);
    }

    @Test
    @DisplayName("CA-2-58 la plantilla XLSX trae las mismas columnas y una hoja de instrucciones")
    void plantillaXlsx() throws Exception {
        byte[] bytes = mvc.perform(get(CARGA + "/plantilla").param("formato", "xlsx").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn().getResponse().getContentAsByteArray();

        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Row cabecera = libro.getSheetAt(0).getRow(0);
            List<String> columnas = new java.util.ArrayList<>();
            cabecera.forEach(c -> columnas.add(c.getStringCellValue()));
            assertThat(String.join(",", columnas)).isEqualTo(CABECERA);
            assertThat(libro.getNumberOfSheets()).isEqualTo(2);
            assertThat(libro.getSheetName(1)).isEqualToIgnoringCase("Instrucciones");
            assertThat(libro.getSheetAt(1).getPhysicalNumberOfRows()).isGreaterThan(5);
        }
    }

    @Test
    @DisplayName("CA-2-58 la plantilla pide un formato válido y es solo para ADMIN")
    void plantillaFormatoYPermisos() throws Exception {
        mvc.perform(get(CARGA + "/plantilla").param("formato", "pdf").with(admin())).andExpect(status().isBadRequest());
        mvc.perform(get(CARGA + "/plantilla").with(admin())).andExpect(status().isBadRequest());
        mvc.perform(get(CARGA + "/plantilla").param("formato", "csv").with(operador())).andExpect(status().isForbidden());
        mvc.perform(get(CARGA + "/plantilla").param("formato", "csv")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------ CA-2-59

    @Test
    @DisplayName("CA-2-59 validar un CSV correcto informa cuántos productos y variantes se crearían y todavía no crea nada")
    void validaCsvSinCrearNada() throws Exception {
        categorias();

        validar(csv(filasBuenas().split("\n"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valida").value(true))
                .andExpect(jsonPath("$.data.filas").value(5))
                .andExpect(jsonPath("$.data.productos").value(2))
                .andExpect(jsonPath("$.data.variantes").value(5))
                .andExpect(jsonPath("$.data.errores", hasSize(0)))
                .andExpect(jsonPath("$.data.validacionId").exists())
                .andExpect(jsonPath("$.data.expiraEn").exists());
        assertThat(productosConPrefijo("CARGA")).isZero();
    }

    @Test
    @DisplayName("CA-2-59 validar un XLSX da el mismo informe que el CSV")
    void validaXlsx() throws Exception {
        categorias();
        byte[] xlsx;
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Productos");
            String[] filas = (CABECERA + "\n" + filasBuenas()).split("\n");
            for (int i = 0; i < filas.length; i++) {
                Row fila = hoja.createRow(i);
                String[] celdas = filas[i].split(",", -1);
                for (int j = 0; j < celdas.length; j++) {
                    if (i > 0 && (j == 3 || j == 7 || j == 8) && !celdas[j].isEmpty()) {
                        fila.createCell(j).setCellValue(Double.parseDouble(celdas[j]));
                    } else if (!celdas[j].isEmpty()) {
                        fila.createCell(j).setCellValue(celdas[j]);
                    }
                }
            }
            libro.write(salida);
            xlsx = salida.toByteArray();
        }

        validar(new MockMultipartFile("archivo", "productos.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valida").value(true))
                .andExpect(jsonPath("$.data.productos").value(2))
                .andExpect(jsonPath("$.data.variantes").value(5));
    }

    @Test
    @DisplayName("CA-2-59 el informe lista por fila los errores: código existente, talla, color o categoría inexistentes, precio inválido y datos que no coinciden")
    void informeDeErrores() throws Exception {
        categorias();
        producto("YAEXISTE", "Producto previo", "30.00", EstadoProducto.BORRADOR);
        sincronizar();

        JsonNode informe = cuerpo(validar(csv(
                "YAEXISTE,Repetido,Desc,30,Cargatops,S,negro,,1",                       // fila 2: código ya existente
                "ERR2,Talla mala,Desc,30,Cargatops,ZZ,negro,,1",                         // fila 3: talla inexistente
                "ERR3,Color malo,Desc,30,Cargatops,S,Turquesa,,1",                         // fila 4: color inexistente
                "ERR4,Categoría mala,Desc,30,NoExiste,S,negro,,1",                       // fila 5: categoría inexistente
                "ERR5,Precio malo,Desc,abc,Cargatops,S,negro,,1",                        // fila 6: precio inválido
                "ERR6,Precio cero,Desc,0,Cargatops,S,negro,,1",                          // fila 7: precio inválido
                "ERR7,Primera fila,Desc,30,Cargatops,S,negro,,1",                        // fila 8
                "ERR7,Otro nombre,Desc,30,Cargatops,M,negro,,1",                         // fila 9: no coincide el nombre
                "ERR8,Variante repetida,Desc,30,Cargatops,S,negro,,1",                   // fila 10
                "ERR8,Variante repetida,Desc,30,Cargatops,S,negro,,2",                   // fila 11: misma variante
                "ERR9,Stock malo,Desc,30,Cargatops,S,negro,,-2",                         // fila 12: stock inválido
                "ERR10,Precio variante malo,Desc,30,Cargatops,S,negro,x,1",              // fila 13
                ",Sin código,Desc,30,Cargatops,S,negro,,1"))                             // fila 14: sin código
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valida").value(false)));

        JsonNode errores = informe.path("data").path("errores");
        assertThat(mensajeDe(errores, 2)).contains("YAEXISTE").contains("ya existe");
        assertThat(columnaDe(errores, 3)).isEqualTo("talla");
        assertThat(mensajeDe(errores, 3)).contains("ZZ");
        assertThat(mensajeDe(errores, 4)).contains("Turquesa");
        assertThat(columnaDe(errores, 4)).isEqualTo("color");
        assertThat(mensajeDe(errores, 5)).contains("NoExiste");
        assertThat(columnaDe(errores, 5)).isEqualTo("categorias");
        assertThat(columnaDe(errores, 6)).isEqualTo("precio_base");
        assertThat(columnaDe(errores, 7)).isEqualTo("precio_base");
        assertThat(mensajeDe(errores, 9)).contains("no coinciden").contains("8");
        assertThat(mensajeDe(errores, 11)).contains("repetida");
        assertThat(columnaDe(errores, 12)).isEqualTo("stock_inicial");
        assertThat(columnaDe(errores, 13)).isEqualTo("precio_variante");
        assertThat(columnaDe(errores, 14)).isEqualTo("codigo");
        assertThat(errores.get(0).path("fila").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(productosConPrefijo("ERR")).isZero();
    }

    private static String mensajeDe(JsonNode errores, int fila) {
        for (JsonNode e : errores) {
            if (e.path("fila").asInt() == fila) {
                return e.path("mensaje").asText();
            }
        }
        return "";
    }

    private static String columnaDe(JsonNode errores, int fila) {
        for (JsonNode e : errores) {
            if (e.path("fila").asInt() == fila) {
                return e.path("columna").asText();
            }
        }
        return "";
    }

    @Test
    @DisplayName("CA-2-59 acepta el CSV con punto y coma (Excel en español), la cabecera en cualquier orden y mayúsculas en talla y color")
    void tolerancia() throws Exception {
        categorias();
        String contenido = "talla;color;codigo;nombre;descripcion;precio_base;categorias;stock_inicial;precio_variante\n"
                + "s;NEGRO;CARGA9;Legging Excel;Desde Excel;49,90;cargatops;4;\n";

        validar(new MockMultipartFile("archivo", "excel.csv", "text/csv", contenido.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valida").value(true))
                .andExpect(jsonPath("$.data.productos").value(1));
    }

    // ------------------------------------------------------------ CA-2-60

    @Test
    @DisplayName("CA-2-60 confirmar crea todos los productos en BORRADOR con sus variantes y una entrada de stock por cada stock_inicial mayor que 0")
    void confirmaYCrea() throws Exception {
        categorias();
        String id = idValidacion(validar(csv(filasBuenas().split("\n"))));

        confirmar(id).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productosCreados", hasSize(2)))
                .andExpect(jsonPath("$.data.productosCreados[0].codigo").value("CARGA1"))
                .andExpect(jsonPath("$.data.variantesCreadas").value(5))
                .andExpect(jsonPath("$.data.entradasStock").value(3));

        sincronizar();
        Producto legging = productos.findByCodigo("CARGA1").orElseThrow();
        assertThat(legging.getEstado()).isEqualTo(EstadoProducto.BORRADOR);
        assertThat(legging.getSlug()).isEqualTo("legging-carga");
        assertThat(legging.getPrecioBase()).isEqualByComparingTo("59.90");
        assertThat(legging.getPrecioDesde()).isEqualByComparingTo("59.90");
        assertThat(legging.getCategorias()).extracting(Categoria::getId).containsExactly(leggings.getId());
        List<Variante> variantesLegging = variantes.findByProductoIdOrderByIdAsc(legging.getId());
        assertThat(variantesLegging).extracting(Variante::getSku).containsExactlyInAnyOrder("CARGA1-NEG-S", "CARGA1-NEG-M", "CARGA1-ROJ-M");
        Variante conPrecio = variantesLegging.stream().filter(v -> v.getSku().equals("CARGA1-NEG-M")).findFirst().orElseThrow();
        assertThat(conPrecio.getPrecio()).isEqualByComparingTo("64.90");
        assertThat(productos.findByCodigo("CARGA2").orElseThrow().getCategorias()).hasSize(2);

        Map<String, Object> entrada = jdbc.queryForMap("SELECT m.* FROM movimiento_stock m WHERE m.variante_id = ?", conPrecio.getId());
        assertThat(entrada).containsEntry("tipo", "ENTRADA").containsEntry("cantidad", 5).containsEntry("saldo_fisico", 5)
                .containsEntry("motivo", "Carga masiva");
        assertThat(entrada.get("usuario_id").toString()).isEqualTo(ADMIN_ID);
        assertThat(jdbc.queryForObject("SELECT fisico FROM stock WHERE variante_id = ?", Integer.class, conPrecio.getId())).isEqualTo(5);
        Variante conCero = variantesLegging.stream().filter(v -> v.getSku().equals("CARGA1-ROJ-M")).findFirst().orElseThrow();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento_stock WHERE variante_id = ?", Integer.class, conCero.getId())).isZero();
        assertThat(jdbc.queryForObject("SELECT fisico FROM stock WHERE variante_id = ?", Integer.class, conCero.getId())).isZero();
    }

    @Test
    @DisplayName("CA-2-60 'desde' del producto creado es el menor precio de sus variantes")
    void precioDesdeDeLoCreado() throws Exception {
        categorias();
        String id = idValidacion(validar(csv("CARGA3,Con precios,Desc,50,Cargatops,S,negro,35.50,1", "CARGA3,Con precios,Desc,50,Cargatops,M,negro,,1")));

        confirmar(id).andExpect(status().isCreated());

        sincronizar();
        assertThat(productos.findByCodigo("CARGA3").orElseThrow().getPrecioDesde()).isEqualByComparingTo("35.50");
    }

    @Test
    @DisplayName("CA-2-60 todo o nada: si al confirmar un código ya fue tomado, no se crea ningún producto y responde 409")
    void todoONada() throws Exception {
        categorias();
        String id = idValidacion(validar(csv(filasBuenas().split("\n"))));
        producto("CARGA2", "Ganó de mano", "20.00", EstadoProducto.BORRADOR);
        sincronizar();

        confirmar(id).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CODIGO_DUPLICADO"));

        assertThat(productosConPrefijo("CARGA1")).isZero();
        assertThat(productosConPrefijo("CARGA2")).isOne();
        // una validación se usa una sola vez
        confirmar(id).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CA-2-60 una validación confirmada no se puede confirmar de nuevo")
    void unSoloUso() throws Exception {
        categorias();
        String id = idValidacion(validar(csv("CARGA4,Un solo uso,Desc,50,Cargatops,S,negro,,1")));

        confirmar(id).andExpect(status().isCreated());
        confirmar(id).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("VALIDACION_VENCIDA"));
        assertThat(productosConPrefijo("CARGA4")).isOne();
    }

    // ------------------------------------------------------------ CA-2-61

    @Test
    @DisplayName("CA-2-61 un informe con errores no se puede confirmar y se indica el motivo")
    void conErroresNoSeConfirma() throws Exception {
        categorias();
        String id = idValidacion(validar(csv("CARGA5,Con error,Desc,50,Cargatops,ZZ,negro,,1")));

        confirmar(id).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_CON_ERRORES"))
                .andExpect(jsonPath("$.mensaje").value(containsString("errores")));
        assertThat(productosConPrefijo("CARGA5")).isZero();
    }

    @Test
    @DisplayName("CA-2-61 una validación inexistente o vencida no se puede confirmar")
    void validacionInexistente() throws Exception {
        confirmar(UUID.randomUUID().toString()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_VENCIDA"))
                .andExpect(jsonPath("$.mensaje").value(containsString("30 minutos")));
        mvc.perform(post(CARGA + "/no-es-un-uuid/confirmar").with(admin())).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-2-61 un archivo de más de 2 000 filas responde 400 con el motivo")
    void masDeDosMilFilas() throws Exception {
        categorias();
        StringBuilder filas = new StringBuilder();
        for (int i = 0; i < 2001; i++) {
            filas.append("MASIVO").append(i).append(",Producto,Desc,10,Cargatops,S,negro,,\n");
        }

        validar(new MockMultipartFile("archivo", "grande.csv", "text/csv", (CABECERA + "\n" + filas).getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("2 000")));
        // 2 000 justas sí se aceptan
        String justas = filas.substring(0, filas.lastIndexOf("MASIVO2000,") );
        validar(new MockMultipartFile("archivo", "justas.csv", "text/csv", (CABECERA + "\n" + justas).getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.filas").value(2000));
    }

    @Test
    @DisplayName("CA-2-61 un archivo de más de 5 MB, de otro tipo o vacío responde 400")
    void archivosInvalidos() throws Exception {
        byte[] enorme = new byte[5 * 1024 * 1024 + 1];
        java.util.Arrays.fill(enorme, (byte) 'a');

        validar(new MockMultipartFile("archivo", "enorme.csv", "text/csv", enorme)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("5 MB")));
        validar(new MockMultipartFile("archivo", "foto.png", "image/png", new byte[] {1, 2, 3})).andExpect(status().isBadRequest());
        validar(new MockMultipartFile("archivo", "vacio.csv", "text/csv", new byte[0])).andExpect(status().isBadRequest());
        validar(csv()).andExpect(status().isBadRequest());
        validar(new MockMultipartFile("archivo", "sin-columnas.csv", "text/csv", "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart(CARGA + "/validar").file(csv("X1,Y,Z,1,,S,negro,,")).with(operador())).andExpect(status().isForbidden());
    }
}
