package com.fitstore.settings.configuracion;

import static com.fitstore.settings.pruebas.Sesiones.admin;
import static com.fitstore.settings.pruebas.Sesiones.operador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.settings.pruebas.PruebaApi;
import com.fitstore.settings.pruebas.Sesiones;
import com.jayway.jsonpath.JsonPath;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuración por secciones con versión optimista, validaciones y eventos
 * (CA-3-05, 06, 08, 10, 11, 13, 15, 16, 17, 18, 19, 36), con la semilla de V3 y la API real.
 */
@Transactional
class ConfiguracionServiceTest extends PruebaApi {

    private static final String ADMIN_SETTINGS = "/api/v1/admin/settings";

    private int versionActual() throws Exception {
        return leer(mvc.perform(get(ADMIN_SETTINGS).with(admin())).andReturn(), "$.data.version");
    }

    private ResultActions guardar(String seccion, Map<String, Object> datos) throws Exception {
        return mvc.perform(put(ADMIN_SETTINGS + "/" + seccion).with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(datos)));
    }

    private Map<String, Object> identidad(int version) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nombre", "Moda Activa");
        m.put("eslogan", "Ropa deportiva de Gamarra");
        m.put("razonSocial", "Moda Activa S.A.C.");
        m.put("ruc", "20123456789");
        m.put("direccionFiscal", "Jr. Gamarra 700, La Victoria, Lima");
        m.put("version", version);
        return m;
    }

    private Map<String, Object> tema(int version, String texto, String fondo, String primario) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("colorPrimario", primario);
        m.put("colorSecundario", "#2B2621");
        m.put("colorAcento", "#4E5B45");
        m.put("colorFondo", fondo);
        m.put("colorTexto", texto);
        m.put("version", version);
        return m;
    }

    private Map<String, Object> contenido(int version) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("heroTitulo", "Nueva colección");
        m.put("heroSubtitulo", "Hecha en Gamarra");
        m.put("heroCtaTexto", "Ver más");
        m.put("heroCtaEnlace", "/productos?categoria=leggings");
        m.put("beneficios", List.of(Map.of("icono", "🚚", "texto", "Envíos a todo el Perú")));
        m.put("email", "ventas@tienda.pe");
        m.put("telefono", "014567890");
        m.put("whatsapp", "987654321");
        m.put("instagramUrl", "https://instagram.com/tienda");
        m.put("facebookUrl", null);
        m.put("tiktokUrl", null);
        m.put("horario", "Lun-Sáb 9:00-19:00");
        m.put("tiendaFisica", null);
        m.put("version", version);
        return m;
    }

    private Map<String, Object> parametros(int version) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("metodosPago", List.of("CULQI", "YAPE"));
        m.put("modalidadesEntrega", List.of("DELIVERY_LOCAL", "RECOJO"));
        m.put("expiracionPedidoMinutos", 45);
        m.put("diasReembolsoPostventa", 15);
        m.put("correosAlertas", List.of("alertas@tienda.pe", "dueno@tienda.pe"));
        m.put("version", version);
        return m;
    }

    // ---------------------------------------------------------------- permisos

    @Test
    @DisplayName("CA-3-06 la configuración completa solo la ve el ADMIN: sin sesión 401 y OPERADOR 403")
    void soloAdminVeLaConfiguracion() throws Exception {
        mvc.perform(get(ADMIN_SETTINGS)).andExpect(status().isUnauthorized());
        mvc.perform(get(ADMIN_SETTINGS).with(operador())).andExpect(status().isForbidden());
        mvc.perform(get(ADMIN_SETTINGS).with(admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.identidad.nombre", is("FitStore")))
                .andExpect(jsonPath("$.data.correosAlertas[0]", is("ventas@tienda.pe")))
                .andExpect(jsonPath("$.data.version", is(0)));
    }

    @Test
    @DisplayName("CA-3-06 el OPERADOR no puede guardar ninguna sección")
    void operadorNoGuarda() throws Exception {
        mvc.perform(put(ADMIN_SETTINGS + "/identidad").with(operador()).contentType(APPLICATION_JSON)
                .content(cuerpo(identidad(0)))).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- identidad (CA-3-06, 08)

    @Test
    @DisplayName("CA-3-06 guarda nombre, eslogan, razón social, RUC y dirección fiscal")
    void guardaLaIdentidad() throws Exception {
        guardar("identidad", identidad(0)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.identidad.nombre", is("Moda Activa")))
                .andExpect(jsonPath("$.data.identidad.ruc", is("20123456789")))
                .andExpect(jsonPath("$.data.identidad.razonSocial", is("Moda Activa S.A.C.")))
                .andExpect(jsonPath("$.data.version", is(1)));
        mvc.perform(get("/api/v1/settings/public")).andExpect(jsonPath("$.data.identidad.nombre", is("Moda Activa")));
    }

    @Test
    @DisplayName("CA-3-06 un RUC inválido responde 400 en el campo ruc")
    void rucInvalido() throws Exception {
        for (String ruc : List.of("123", "2012345678a", "201234567890", "")) {
            Map<String, Object> datos = identidad(0);
            datos.put("ruc", ruc);
            guardar("identidad", datos).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo", is("VALIDACION")))
                    .andExpect(jsonPath("$.errores[?(@.campo=='ruc')]", hasSize(org.hamcrest.Matchers.greaterThan(0))));
        }
        assertThat(versionActual()).isZero();
    }

    @Test
    @DisplayName("CA-3-06 el nombre y la razón social son obligatorios")
    void camposObligatorios() throws Exception {
        Map<String, Object> datos = identidad(0);
        datos.put("nombre", "");
        datos.put("razonSocial", null);
        guardar("identidad", datos).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("nombre")))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("razonSocial")));
    }

    @Test
    @DisplayName("CA-3-08 si otro admin guardó la sección después de abrirla, responde 409 y no pisa su cambio")
    void versionDesactualizada() throws Exception {
        int abierta = versionActual();
        Map<String, Object> delOtro = identidad(abierta);
        delOtro.put("nombre", "Cambio del otro admin");
        guardar("identidad", delOtro).andExpect(status().isOk());

        Map<String, Object> mio = identidad(abierta);
        mio.put("nombre", "Mi cambio tardío");
        guardar("identidad", mio).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("CONFLICTO_VERSION")))
                .andExpect(jsonPath("$.mensaje", is("Otro administrador modificó la configuración; recarga la página")));

        mvc.perform(get(ADMIN_SETTINGS).with(admin()))
                .andExpect(jsonPath("$.data.identidad.nombre", is("Cambio del otro admin")));
    }

    @Test
    @DisplayName("CA-3-08 cada guardado sube la versión y devuelve la nueva")
    void cadaGuardadoSubeLaVersion() throws Exception {
        guardar("identidad", identidad(0)).andExpect(jsonPath("$.data.version", is(1)));
        guardar("tema", tema(1, "#26221D", "#FAF9F7", "#C9B28C")).andExpect(jsonPath("$.data.version", is(2)));
        guardar("parametros", parametros(2)).andExpect(jsonPath("$.data.version", is(3)));
    }

    // ---------------------------------------------------------------- tema (CA-3-10, 11)

    @Test
    @DisplayName("CA-3-11 guardar el tema publica ConfiguracionActualizada y la tienda lo muestra al recargar")
    void guardaElTemaYPublicaElEvento() throws Exception {
        guardar("tema", tema(0, "#111111", "#FFFFFF", "#1E3A8A")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tema.colorPrimario", is("#1E3A8A")))
                .andExpect(jsonPath("$.data.advertencias", empty()));

        mvc.perform(get("/api/v1/settings/public")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tema.colorPrimario", is("#1E3A8A")))
                .andExpect(jsonPath("$.data.tema.colorFondo", is("#FFFFFF")));

        List<String> eventos = eventosDelTipo("ConfiguracionActualizada");
        assertThat(eventos).hasSize(1);
        assertThat((int) JsonPath.read(eventos.get(0), "$.version")).isEqualTo(1);
        assertThat((List<String>) JsonPath.read(eventos.get(0), "$.secciones")).containsExactly("TEMA");
        assertThat((String) JsonPath.read(eventos.get(0), "$.configuracion.tema.colorPrimario")).isEqualTo("#1E3A8A");
    }

    @Test
    @DisplayName("CA-3-10 un contraste texto/fondo menor a 4.5:1 se guarda igual y avisa con la razón calculada")
    void advierteElContrasteBajo() throws Exception {
        guardar("tema", tema(0, "#9A9A9A", "#FFFFFF", "#1E3A8A")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tema.colorTexto", is("#9A9A9A")))
                .andExpect(jsonPath("$.data.advertencias", hasSize(1)))
                .andExpect(jsonPath("$.data.advertencias[0]", startsWith("Contraste texto/fondo 2.8:1")));
        assertThat(versionActual()).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-3-10 avisa si ni el blanco ni el color de texto se leen sobre el primario")
    void advierteElPrimarioSinTextoLegible() throws Exception {
        guardar("tema", tema(0, "#777777", "#FFFFFF", "#808080")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.advertencias[?(@ =~ /.*primario.*/)]", hasSize(1)));
    }

    @Test
    @DisplayName("CA-3-10 el tema por defecto de FitStore no genera advertencias")
    void temaPorDefectoSinAdvertencias() throws Exception {
        guardar("tema", tema(0, "#26221D", "#FAF9F7", "#C9B28C")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.advertencias", empty()));
    }

    @Test
    @DisplayName("CA-3-10 un color con formato inválido sí se rechaza con 400 en su campo")
    void colorInvalido() throws Exception {
        guardar("tema", tema(0, "rojo", "#FFFFFF", "#1E3A8A")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("colorTexto")));
    }

    // ---------------------------------------------------------------- contenido (CA-3-13, 15, 16)

    @Test
    @DisplayName("CA-3-13 guarda el hero con un enlace interno o una URL https y la landing lo muestra (CA-3-05)")
    void guardaElHero() throws Exception {
        guardar("contenido", contenido(0)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido.heroTitulo", is("Nueva colección")));
        Map<String, Object> https = contenido(1);
        https.put("heroCtaEnlace", "https://tienda.pe/oferta");
        guardar("contenido", https).andExpect(status().isOk());

        mvc.perform(get("/api/v1/settings/public"))
                .andExpect(jsonPath("$.data.contenido.heroTitulo", is("Nueva colección")))
                .andExpect(jsonPath("$.data.contenido.heroCtaEnlace", is("https://tienda.pe/oferta")));
    }

    @Test
    @DisplayName("CA-3-13 un enlace del hero que no es ruta de la tienda ni https se rechaza (javascript:, http:, //)")
    void enlaceDelHeroInvalido() throws Exception {
        for (String enlace : List.of("javascript:alert(1)", "http://tienda.pe", "//malo.com", "productos", "/a b")) {
            Map<String, Object> datos = contenido(0);
            datos.put("heroCtaEnlace", enlace);
            guardar("contenido", datos).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores[*].campo", hasItem("heroCtaEnlace")));
        }
    }

    @Test
    @DisplayName("CA-3-15 admite de 0 a 3 beneficios y la franja vacía queda como lista vacía")
    void beneficiosDeCeroATres() throws Exception {
        Map<String, Object> tres = contenido(0);
        tres.put("beneficios", List.of(Map.of("icono", "🚚", "texto", "Envíos"), Map.of("icono", "↩", "texto", "Cambios"),
                Map.of("icono", "🔒", "texto", "Pago seguro")));
        guardar("contenido", tres).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido.beneficios", hasSize(3)))
                .andExpect(jsonPath("$.data.contenido.beneficios[1].texto", is("Cambios")));

        Map<String, Object> ninguno = contenido(1);
        ninguno.put("beneficios", List.of());
        guardar("contenido", ninguno).andExpect(status().isOk());
        mvc.perform(get("/api/v1/settings/public")).andExpect(jsonPath("$.data.contenido.beneficios", empty()));
    }

    @Test
    @DisplayName("CA-3-15 más de 3 beneficios o un texto de más de 60 caracteres se rechazan")
    void beneficiosInvalidos() throws Exception {
        Map<String, Object> cuatro = contenido(0);
        cuatro.put("beneficios", List.of(Map.of("icono", "a", "texto", "1"), Map.of("icono", "b", "texto", "2"),
                Map.of("icono", "c", "texto", "3"), Map.of("icono", "d", "texto", "4")));
        guardar("contenido", cuatro).andExpect(status().isBadRequest());

        Map<String, Object> largo = contenido(0);
        largo.put("beneficios", List.of(Map.of("icono", "🚚", "texto", "x".repeat(61))));
        guardar("contenido", largo).andExpect(status().isBadRequest());

        Map<String, Object> sinTexto = contenido(0);
        sinTexto.put("beneficios", List.of(Map.of("icono", "🚚", "texto", " ")));
        guardar("contenido", sinTexto).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-3-16 valida correo, WhatsApp y URLs de redes")
    void contactoYRedesInvalidos() throws Exception {
        Map<String, Object> correo = contenido(0);
        correo.put("email", "no-es-correo");
        guardar("contenido", correo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("email")));

        Map<String, Object> whatsapp = contenido(0);
        whatsapp.put("whatsapp", "12345");
        guardar("contenido", whatsapp).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("whatsapp")));

        Map<String, Object> red = contenido(0);
        red.put("instagramUrl", "http://instagram.com/tienda");
        guardar("contenido", red).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("instagramUrl")));
    }

    @Test
    @DisplayName("CA-3-16 guarda la tienda física con su ubigeo y la landing recibe el nombre del distrito")
    void tiendaFisicaConUbigeo() throws Exception {
        Map<String, Object> datos = contenido(0);
        datos.put("tiendaFisica", Map.of("direccion", "Jr. Gamarra 700, tienda 210", "ubigeo", "150115",
                "referencia", "Galería Gamarra"));
        guardar("contenido", datos).andExpect(status().isOk());
        mvc.perform(get("/api/v1/settings/public"))
                .andExpect(jsonPath("$.data.contenido.tiendaFisica.direccion", is("Jr. Gamarra 700, tienda 210")))
                .andExpect(jsonPath("$.data.contenido.tiendaFisica.ubigeo", is("150115")))
                .andExpect(jsonPath("$.data.contenido.tiendaFisica.ubigeoNombre", is("La Victoria, Lima, Lima")))
                .andExpect(jsonPath("$.data.contenido.tiendaFisica.referencia", is("Galería Gamarra")));

        Map<String, Object> sinTienda = contenido(1);
        guardar("contenido", sinTienda).andExpect(status().isOk());
        mvc.perform(get("/api/v1/settings/public")).andExpect(jsonPath("$.data.contenido.tiendaFisica", nullValue()));
    }

    @Test
    @DisplayName("CA-3-16 un ubigeo que no existe en la tienda física se rechaza")
    void ubigeoInexistente() throws Exception {
        Map<String, Object> datos = contenido(0);
        datos.put("tiendaFisica", Map.of("direccion", "Jr. Gamarra 700", "ubigeo", "999999"));
        guardar("contenido", datos).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("tiendaFisica.ubigeo")));
    }

    // ---------------------------------------------------------------- parámetros (CA-3-17, 18, 19)

    @Test
    @DisplayName("CA-3-17 guarda medios de pago, entregas, expiración, reembolso y correos; la moneda es PEN")
    void guardaLosParametros() throws Exception {
        guardar("parametros", parametros(0)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parametros.metodosPago", containsInAnyOrder("CULQI", "YAPE")))
                .andExpect(jsonPath("$.data.parametros.expiracionPedidoMinutos", is(45)))
                .andExpect(jsonPath("$.data.parametros.diasReembolsoPostventa", is(15)))
                .andExpect(jsonPath("$.data.parametros.moneda", is("PEN")))
                .andExpect(jsonPath("$.data.correosAlertas", hasSize(2)));
    }

    @Test
    @DisplayName("CA-3-17 debe haber al menos un medio de pago y una modalidad de entrega")
    void alMenosUnoDeCada() throws Exception {
        Map<String, Object> sinPagos = parametros(0);
        sinPagos.put("metodosPago", List.of());
        guardar("parametros", sinPagos).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("metodosPago")));

        Map<String, Object> sinEntregas = parametros(0);
        sinEntregas.put("modalidadesEntrega", List.of());
        guardar("parametros", sinEntregas).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("modalidadesEntrega")));
    }

    @Test
    @DisplayName("CA-3-17 la expiración del pedido va de 10 a 120 minutos")
    void rangoDeExpiracion() throws Exception {
        for (int minutos : List.of(9, 121, 0)) {
            Map<String, Object> datos = parametros(0);
            datos.put("expiracionPedidoMinutos", minutos);
            guardar("parametros", datos).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores[*].campo", hasItem("expiracionPedidoMinutos")));
        }
        for (int minutos : List.of(10, 120)) {
            Map<String, Object> datos = parametros(versionActual());
            datos.put("expiracionPedidoMinutos", minutos);
            guardar("parametros", datos).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("CA-3-17 los días de reembolso van de 0 a 365 (0 = sin límite)")
    void rangoDeReembolso() throws Exception {
        for (int dias : List.of(-1, 366)) {
            Map<String, Object> datos = parametros(0);
            datos.put("diasReembolsoPostventa", dias);
            guardar("parametros", datos).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores[*].campo", hasItem("diasReembolsoPostventa")));
        }
        Map<String, Object> sinLimite = parametros(0);
        sinLimite.put("diasReembolsoPostventa", 0);
        guardar("parametros", sinLimite).andExpect(status().isOk());
        mvc.perform(get("/api/v1/settings/public"))
                .andExpect(jsonPath("$.data.parametros.diasReembolsoPostventa", is(0)));
    }

    @Test
    @DisplayName("CA-3-17 los correos para alertas son de 1 a 5 y válidos")
    void correosDeAlertas() throws Exception {
        Map<String, Object> ninguno = parametros(0);
        ninguno.put("correosAlertas", List.of());
        guardar("parametros", ninguno).andExpect(status().isBadRequest());

        Map<String, Object> seis = parametros(0);
        seis.put("correosAlertas", List.of("a@t.pe", "b@t.pe", "c@t.pe", "d@t.pe", "e@t.pe", "f@t.pe"));
        guardar("parametros", seis).andExpect(status().isBadRequest());

        Map<String, Object> invalido = parametros(0);
        invalido.put("correosAlertas", List.of("no-es-correo"));
        guardar("parametros", invalido).andExpect(status().isBadRequest());

        Map<String, Object> cinco = parametros(0);
        cinco.put("correosAlertas", List.of("a@t.pe", "b@t.pe", "c@t.pe", "d@t.pe", "e@t.pe"));
        guardar("parametros", cinco).andExpect(status().isOk());
    }

    @Test
    @DisplayName("CA-3-18 al deshabilitar un medio de pago o una modalidad, /settings/public lo refleja")
    void deshabilitarSeReflejaEnLoPublico() throws Exception {
        mvc.perform(get("/api/v1/settings/public"))
                .andExpect(jsonPath("$.data.parametros.metodosPago", hasSize(3)))
                .andExpect(jsonPath("$.data.parametros.modalidadesEntrega", hasSize(3)));

        guardar("parametros", parametros(0)).andExpect(status().isOk());

        mvc.perform(get("/api/v1/settings/public"))
                .andExpect(jsonPath("$.data.parametros.metodosPago", containsInAnyOrder("CULQI", "YAPE")))
                .andExpect(jsonPath("$.data.parametros.metodosPago", not(hasItem("MERCADO_PAGO"))))
                .andExpect(jsonPath("$.data.parametros.modalidadesEntrega", containsInAnyOrder("DELIVERY_LOCAL", "RECOJO")))
                .andExpect(jsonPath("$.data.parametros.expiracionPedidoMinutos", is(45)));
    }

    @Test
    @DisplayName("CA-3-19 los correos para alertas viajan en ConfiguracionActualizada pero no salen en /settings/public")
    void correosSoloEnElEvento() throws Exception {
        guardar("parametros", parametros(0)).andExpect(status().isOk());

        String publica = mvc.perform(get("/api/v1/settings/public")).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        assertThat(publica).doesNotContain("correosAlertas").doesNotContain("alertas@tienda.pe")
                .doesNotContain("dueno@tienda.pe");

        List<String> eventos = eventosDelTipo("ConfiguracionActualizada");
        assertThat(eventos).hasSize(1);
        assertThat((List<String>) JsonPath.read(eventos.get(0), "$.correosAlertas"))
                .containsExactly("alertas@tienda.pe", "dueno@tienda.pe");
        assertThat((List<String>) JsonPath.read(eventos.get(0), "$.secciones")).containsExactly("PARAMETROS");
        assertThat((Object) JsonPath.read(eventos.get(0), "$.configuracion.parametros.metodosPago")).isNotNull();
    }

    // ---------------------------------------------------------------- lo público (CA-3-36)

    @Test
    @DisplayName("CA-3-36 /settings/public nunca incluye correos para alertas, autor de cambios, versión ni secretos")
    void loPublicoNoExponeDatosInternos() throws Exception {
        guardar("identidad", identidad(0)).andExpect(status().isOk());
        String publica = mvc.perform(get("/api/v1/settings/public")).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        assertThat(publica).doesNotContain("correosAlertas").doesNotContain("actualizadoPor")
                .doesNotContain("actualizadoEn").doesNotContain("\"version\"").doesNotContain("ventas@tienda.pe\"]")
                .doesNotContain(Sesiones.ID_ADMIN);
        mvc.perform(get("/api/v1/settings/public")).andExpect(jsonPath("$.data.identidad.ruc", is("20123456789")))
                .andExpect(jsonPath("$.data.identidad.direccionFiscal", is("Jr. Gamarra 700, La Victoria, Lima")));
    }

    @Test
    @DisplayName("CA-3-05 la semilla de la marca responde en /settings/public sin sesión")
    void laSemillaEsPublica() throws Exception {
        mvc.perform(get("/api/v1/settings/public")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.identidad.nombre", is("FitStore")))
                .andExpect(jsonPath("$.data.tema.colorPrimario", is("#C9B28C")))
                .andExpect(jsonPath("$.data.contenido.heroTitulo", is("Muévete con estilo")))
                .andExpect(jsonPath("$.data.parametros.moneda", is("PEN")));
    }
}
