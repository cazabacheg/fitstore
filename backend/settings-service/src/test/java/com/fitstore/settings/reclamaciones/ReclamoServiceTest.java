package com.fitstore.settings.reclamaciones;

import static com.fitstore.settings.pruebas.Sesiones.ID_ADMIN;
import static com.fitstore.settings.pruebas.Sesiones.admin;
import static com.fitstore.settings.pruebas.Sesiones.operador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.settings.pruebas.PruebaApi;
import com.jayway.jsonpath.JsonPath;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Libro de Reclamaciones: registro público, bandeja, transiciones y permisos (CA-3-23 a CA-3-25, CA-3-28 a CA-3-31).
 * El reloj de la prueba marca el lunes 19-oct-2026.
 */
@Transactional
class ReclamoServiceTest extends PruebaApi {

    private static final String RECLAMOS = "/api/v1/reclamos";
    private static final String ADMIN_RECLAMOS = "/api/v1/admin/reclamos";

    private Map<String, Object> reclamo() {
        Map<String, Object> consumidor = new LinkedHashMap<>();
        consumidor.put("nombres", "Rosa");
        consumidor.put("apellidos", "Díaz");
        consumidor.put("tipoDocumento", "DNI");
        consumidor.put("numeroDocumento", "87654321");
        consumidor.put("domicilio", "Calle Mercaderes 456, Cercado, Arequipa");
        consumidor.put("telefono", "912345678");
        consumidor.put("email", "rosa.diaz@correo.com");
        consumidor.put("esMenor", false);
        Map<String, Object> bien = new LinkedHashMap<>();
        bien.put("tipo", "PRODUCTO");
        bien.put("descripcion", "Casaca Wind talla M negra");
        bien.put("montoReclamado", 89.9);
        bien.put("pedidoCodigo", "FS-000118");
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("tipo", "RECLAMO");
        datos.put("consumidor", consumidor);
        datos.put("bien", bien);
        datos.put("detalle", "La casaca llegó con el cierre roto.");
        datos.put("pedidoConsumidor", "Cambio por una nueva o devolución del dinero.");
        return datos;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> sub(Map<String, Object> datos, String clave) {
        return (Map<String, Object>) datos.get(clave);
    }

    private ResultActions registrar(Map<String, Object> datos) throws Exception {
        return mvc.perform(post(RECLAMOS).contentType(APPLICATION_JSON).content(cuerpo(datos)));
    }

    private long registrarYObtenerId(Map<String, Object> datos) throws Exception {
        registrar(datos).andExpect(status().isCreated());
        em.flush();
        return jdbc.queryForObject("SELECT max(id) FROM reclamo", Long.class);
    }

    private long reclamoConLimite(String fechaLimite, String nombres) throws Exception {
        Map<String, Object> datos = reclamo();
        sub(datos, "consumidor").put("nombres", nombres);
        long id = registrarYObtenerId(datos);
        jdbc.update("UPDATE reclamo SET fecha_limite = ?::date WHERE id = ?", fechaLimite, id);
        return id;
    }

    // ---------------------------------------------------------------- registro (CA-3-23, 24, 25)

    @Test
    @DisplayName("CA-3-25 un reclamo válido recibe el siguiente número del año y 15 días hábiles de plazo, sin sesión")
    void registraUnReclamo() throws Exception {
        registrar(reclamo()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.numero", is("RC-2026-000001")))
                .andExpect(jsonPath("$.data.fechaLimite", is("2026-11-09")))
                .andExpect(jsonPath("$.data.creadoEn", is("2026-10-19T15:00:00Z")));

        registrar(reclamo()).andExpect(jsonPath("$.data.numero", is("RC-2026-000002")));
        em.flush();
        assertThat(jdbc.queryForObject("SELECT estado FROM reclamo WHERE numero = 'RC-2026-000001'", String.class))
                .isEqualTo("REGISTRADO");
        assertThat(jdbc.queryForObject("SELECT ultimo FROM correlativo_reclamo WHERE anio = 2026", Integer.class))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("CA-3-25 al registrar se publica ReclamoRegistrado con el consumidor, el bien y la fecha límite")
    void publicaReclamoRegistrado() throws Exception {
        registrar(reclamo()).andExpect(status().isCreated());

        List<String> eventos = eventosDelTipo("ReclamoRegistrado");
        assertThat(eventos).hasSize(1);
        String e = eventos.get(0);
        assertThat((String) JsonPath.read(e, "$.numero")).isEqualTo("RC-2026-000001");
        assertThat((String) JsonPath.read(e, "$.tipo")).isEqualTo("RECLAMO");
        assertThat((String) JsonPath.read(e, "$.consumidor.nombre")).isEqualTo("Rosa Díaz");
        assertThat((String) JsonPath.read(e, "$.consumidor.email")).isEqualTo("rosa.diaz@correo.com");
        assertThat((String) JsonPath.read(e, "$.bien.descripcion")).isEqualTo("Casaca Wind talla M negra");
        assertThat((String) JsonPath.read(e, "$.detalle")).isEqualTo("La casaca llegó con el cierre roto.");
        assertThat((String) JsonPath.read(e, "$.pedidoConsumidor")).startsWith("Cambio por una nueva");
        assertThat((String) JsonPath.read(e, "$.fechaLimite")).isEqualTo("2026-11-09");
        assertThat((String) JsonPath.read(e, "$.creadoEn")).startsWith("2026-10-19T15:00:00");
    }

    @Test
    @DisplayName("CA-3-25 guarda la IP de quien registra (primer valor de X-Forwarded-For) para el staff")
    void guardaLaIp() throws Exception {
        mvc.perform(post(RECLAMOS).contentType(APPLICATION_JSON).header("X-Forwarded-For", "203.0.113.5, 10.0.0.1")
                .content(cuerpo(reclamo()))).andExpect(status().isCreated());
        em.flush();
        assertThat(jdbc.queryForObject("SELECT ip FROM reclamo", String.class)).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("CA-3-24 si es menor de edad se exige el nombre del padre, madre o apoderado")
    void menorDeEdadExigeApoderado() throws Exception {
        Map<String, Object> datos = reclamo();
        sub(datos, "consumidor").put("esMenor", true);
        registrar(datos).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("consumidor.apoderadoNombre")));

        sub(datos, "consumidor").put("apoderadoNombre", "  ");
        registrar(datos).andExpect(status().isBadRequest());

        sub(datos, "consumidor").put("apoderadoNombre", "Marta Díaz Pérez");
        registrar(datos).andExpect(status().isCreated());
        em.flush();
        assertThat(jdbc.queryForObject("SELECT apoderado_nombre FROM reclamo", String.class))
                .isEqualTo("Marta Díaz Pérez");
    }

    @Test
    @DisplayName("CA-3-24 si es mayor de edad el apoderado es opcional")
    void mayorDeEdadSinApoderado() throws Exception {
        registrar(reclamo()).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CA-3-23 valida el documento: DNI de 8 dígitos o carné de extranjería de 8 a 12 caracteres")
    void validaElDocumento() throws Exception {
        for (String dni : List.of("1234567", "123456789", "1234567a")) {
            Map<String, Object> datos = reclamo();
            sub(datos, "consumidor").put("numeroDocumento", dni);
            registrar(datos).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores[*].campo", hasItem("consumidor.numeroDocumento")));
        }
        Map<String, Object> ce = reclamo();
        sub(ce, "consumidor").put("tipoDocumento", "CE");
        sub(ce, "consumidor").put("numeroDocumento", "CE0012345X");
        registrar(ce).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CA-3-23 valida correo, monto, código de pedido y los campos obligatorios de la hoja")
    void validaLaHoja() throws Exception {
        Map<String, Object> correo = reclamo();
        sub(correo, "consumidor").put("email", "no-es-correo");
        registrar(correo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("consumidor.email")));

        Map<String, Object> monto = reclamo();
        sub(monto, "bien").put("montoReclamado", -1);
        registrar(monto).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("bien.montoReclamado")));

        Map<String, Object> pedido = reclamo();
        sub(pedido, "bien").put("pedidoCodigo", "FS-12");
        registrar(pedido).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("bien.pedidoCodigo")));

        Map<String, Object> sinDetalle = reclamo();
        sinDetalle.put("detalle", "corto");
        sinDetalle.put("pedidoConsumidor", "");
        registrar(sinDetalle).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("detalle")))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("pedidoConsumidor")));

        Map<String, Object> sinTipo = reclamo();
        sinTipo.remove("tipo");
        registrar(sinTipo).andExpect(status().isBadRequest());

        Map<String, Object> sinConsumidor = reclamo();
        sinConsumidor.remove("consumidor");
        registrar(sinConsumidor).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reclamo", Integer.class)).isZero();
    }

    @Test
    @DisplayName("CA-3-23 el monto y el código de pedido son opcionales y una queja se registra igual")
    void montoYPedidoOpcionales() throws Exception {
        Map<String, Object> datos = reclamo();
        datos.put("tipo", "QUEJA");
        sub(datos, "bien").put("montoReclamado", null);
        sub(datos, "bien").put("pedidoCodigo", null);
        registrar(datos).andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------- bandeja (CA-3-28)

    @Test
    @DisplayName("CA-3-28 la bandeja pide sesión y la ven el OPERADOR y el ADMIN")
    void permisosDeLaBandeja() throws Exception {
        mvc.perform(get(ADMIN_RECLAMOS)).andExpect(status().isUnauthorized());
        mvc.perform(get(ADMIN_RECLAMOS).with(operador())).andExpect(status().isOk());
        mvc.perform(get(ADMIN_RECLAMOS).with(admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElementos", is(0)));
    }

    @Test
    @DisplayName("CA-3-28 lista con los datos de la fila, paginada y la más reciente primero")
    void listaPaginada() throws Exception {
        registrar(reclamo()).andExpect(status().isCreated());
        registrar(reclamo()).andExpect(status().isCreated());
        registrar(reclamo()).andExpect(status().isCreated());
        em.flush();
        jdbc.update("UPDATE reclamo SET creado_en = creado_en - (interval '1 day' * (3 - correlativo))");

        mvc.perform(get(ADMIN_RECLAMOS).with(operador()).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagina", is(0)))
                .andExpect(jsonPath("$.data.tamano", is(2)))
                .andExpect(jsonPath("$.data.totalElementos", is(3)))
                .andExpect(jsonPath("$.data.totalPaginas", is(2)))
                .andExpect(jsonPath("$.data.contenido", hasSize(2)))
                .andExpect(jsonPath("$.data.contenido[0].numero", is("RC-2026-000003")))
                .andExpect(jsonPath("$.data.contenido[0].consumidorNombre", is("Rosa Díaz")))
                .andExpect(jsonPath("$.data.contenido[0].tipo", is("RECLAMO")))
                .andExpect(jsonPath("$.data.contenido[0].estado", is("REGISTRADO")))
                .andExpect(jsonPath("$.data.contenido[0].fechaLimite", is("2026-11-09")))
                .andExpect(jsonPath("$.data.contenido[0].id").isNumber())
                .andExpect(jsonPath("$.data.contenido[0].consumidor").doesNotExist());
        mvc.perform(get(ADMIN_RECLAMOS).with(operador()).param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.data.contenido", hasSize(1)))
                .andExpect(jsonPath("$.data.contenido[0].numero", is("RC-2026-000001")));
    }

    @Test
    @DisplayName("CA-3-28 filtra por estado y por tipo")
    void filtraPorEstadoYTipo() throws Exception {
        long primero = registrarYObtenerId(reclamo());
        Map<String, Object> queja = reclamo();
        queja.put("tipo", "QUEJA");
        registrarYObtenerId(queja);
        mvc.perform(patch(ADMIN_RECLAMOS + "/" + primero + "/en-atencion").with(admin())).andExpect(status().isOk());

        mvc.perform(get(ADMIN_RECLAMOS).with(admin()).param("estado", "EN_ATENCION"))
                .andExpect(jsonPath("$.data.contenido", hasSize(1)))
                .andExpect(jsonPath("$.data.contenido[0].numero", is("RC-2026-000001")));
        mvc.perform(get(ADMIN_RECLAMOS).with(admin()).param("estado", "REGISTRADO"))
                .andExpect(jsonPath("$.data.contenido[*].numero", contains("RC-2026-000002")));
        mvc.perform(get(ADMIN_RECLAMOS).with(admin()).param("tipo", "QUEJA"))
                .andExpect(jsonPath("$.data.contenido[*].numero", contains("RC-2026-000002")));
        mvc.perform(get(ADMIN_RECLAMOS).with(admin()).param("tipo", "OTRO")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-3-28 busca por número, nombre o documento")
    void buscaPorTexto() throws Exception {
        registrar(reclamo()).andExpect(status().isCreated());
        Map<String, Object> otro = reclamo();
        sub(otro, "consumidor").put("nombres", "Luis");
        sub(otro, "consumidor").put("apellidos", "Quispe");
        sub(otro, "consumidor").put("numeroDocumento", "11223344");
        registrar(otro).andExpect(status().isCreated());

        mvc.perform(get(ADMIN_RECLAMOS).with(admin()).param("q", "000002"))
                .andExpect(jsonPath("$.data.contenido[*].numero", contains("RC-2026-000002")));
        mvc.perform(get(ADMIN_RECLAMOS).with(admin()).param("q", "quispe"))
                .andExpect(jsonPath("$.data.contenido[*].numero", contains("RC-2026-000002")));
        mvc.perform(get(ADMIN_RECLAMOS).with(admin()).param("q", "87654321"))
                .andExpect(jsonPath("$.data.contenido[*].numero", contains("RC-2026-000001")));
        mvc.perform(get(ADMIN_RECLAMOS).with(admin()).param("q", "inexistente"))
                .andExpect(jsonPath("$.data.contenido", hasSize(0)));
    }

    @Test
    @DisplayName("CA-3-28 filtra por rango de fechas de registro")
    void filtraPorFechas() throws Exception {
        long viejo = registrarYObtenerId(reclamo());
        registrarYObtenerId(reclamo());
        jdbc.update("UPDATE reclamo SET creado_en = '2026-09-01T12:00:00Z' WHERE id = ?", viejo);

        mvc.perform(get(ADMIN_RECLAMOS).with(admin()).param("desde", "2026-10-01"))
                .andExpect(jsonPath("$.data.contenido[*].numero", contains("RC-2026-000002")));
        mvc.perform(get(ADMIN_RECLAMOS).with(admin()).param("hasta", "2026-09-30"))
                .andExpect(jsonPath("$.data.contenido[*].numero", contains("RC-2026-000001")));
        mvc.perform(get(ADMIN_RECLAMOS).with(admin()).param("desde", "2026-09-01").param("hasta", "2026-09-01"))
                .andExpect(jsonPath("$.data.contenido[*].numero", contains("RC-2026-000001")));
    }

    @Test
    @DisplayName("CA-3-28 marca 'por vencer' (3 días hábiles o menos) y 'vencido', y los filtra")
    void indicadoresDePlazo() throws Exception {
        reclamoConLimite("2026-10-16", "Vencido");     // viernes pasado
        reclamoConLimite("2026-10-19", "Hoy");         // vence hoy: por vencer
        reclamoConLimite("2026-10-22", "TresDias");    // jueves: 3 días hábiles
        reclamoConLimite("2026-10-23", "CuatroDias");  // viernes: 4 días hábiles, aún no
        reclamoConLimite("2026-11-09", "Lejano");

        String lista = mvc.perform(get(ADMIN_RECLAMOS).with(admin()).param("size", "50")).andReturn().getResponse()
                .getContentAsString();
        assertThat((List<Boolean>) JsonPath.read(lista, "$.data.contenido[?(@.fechaLimite=='2026-10-16')].vencido"))
                .containsExactly(true);
        assertThat((List<Boolean>) JsonPath.read(lista, "$.data.contenido[?(@.fechaLimite=='2026-10-16')].porVencer"))
                .containsExactly(false);
        assertThat((List<Boolean>) JsonPath.read(lista, "$.data.contenido[?(@.fechaLimite=='2026-10-19')].porVencer"))
                .containsExactly(true);
        assertThat((List<Boolean>) JsonPath.read(lista, "$.data.contenido[?(@.fechaLimite=='2026-10-22')].porVencer"))
                .containsExactly(true);
        assertThat((List<Boolean>) JsonPath.read(lista, "$.data.contenido[?(@.fechaLimite=='2026-10-23')].porVencer"))
                .containsExactly(false);
        assertThat((List<Boolean>) JsonPath.read(lista, "$.data.contenido[?(@.fechaLimite=='2026-11-09')].vencido"))
                .containsExactly(false);

        mvc.perform(get(ADMIN_RECLAMOS).with(admin()).param("vencidos", "true"))
                .andExpect(jsonPath("$.data.contenido", hasSize(1)))
                .andExpect(jsonPath("$.data.contenido[0].fechaLimite", is("2026-10-16")));
        mvc.perform(get(ADMIN_RECLAMOS).with(admin()).param("porVencer", "true"))
                .andExpect(jsonPath("$.data.contenido", hasSize(2)));
    }

    @Test
    @DisplayName("CA-3-28 un reclamo respondido ya no está vencido ni por vencer aunque pasó su fecha límite")
    void respondidoNoVence() throws Exception {
        long id = reclamoConLimite("2026-10-16", "Respondido");
        mvc.perform(post(ADMIN_RECLAMOS + "/" + id + "/respuesta").with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(Map.of("respuesta", "Procedemos con el cambio del producto.")))).andExpect(status().isOk());

        mvc.perform(get(ADMIN_RECLAMOS).with(admin()))
                .andExpect(jsonPath("$.data.contenido[0].vencido", is(false)))
                .andExpect(jsonPath("$.data.contenido[0].porVencer", is(false)));
        mvc.perform(get(ADMIN_RECLAMOS).with(admin()).param("vencidos", "true"))
                .andExpect(jsonPath("$.data.contenido", hasSize(0)));
    }

    // ---------------------------------------------------------------- detalle y transiciones (CA-3-29, 30, 31)

    @Test
    @DisplayName("CA-3-28 el detalle trae la hoja completa; el OPERADOR también la ve")
    void detalleCompleto() throws Exception {
        long id = registrarYObtenerId(reclamo());

        mvc.perform(get(ADMIN_RECLAMOS + "/" + id).with(operador())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.numero", is("RC-2026-000001")))
                .andExpect(jsonPath("$.data.consumidor.nombres", is("Rosa")))
                .andExpect(jsonPath("$.data.consumidor.numeroDocumento", is("87654321")))
                .andExpect(jsonPath("$.data.consumidor.domicilio", is("Calle Mercaderes 456, Cercado, Arequipa")))
                .andExpect(jsonPath("$.data.consumidor.esMenor", is(false)))
                .andExpect(jsonPath("$.data.bien.tipo", is("PRODUCTO")))
                .andExpect(jsonPath("$.data.bien.montoReclamado", is(89.9)))
                .andExpect(jsonPath("$.data.bien.pedidoCodigo", is("FS-000118")))
                .andExpect(jsonPath("$.data.detalle", is("La casaca llegó con el cierre roto.")))
                .andExpect(jsonPath("$.data.respuesta", nullValue()))
                .andExpect(jsonPath("$.data.porVencer", is(false)));
        mvc.perform(get(ADMIN_RECLAMOS + "/999999").with(admin())).andExpect(status().isNotFound());
        mvc.perform(get(ADMIN_RECLAMOS + "/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CA-3-29 tomar un reclamo lo pasa a EN_ATENCION (paso opcional)")
    void tomarUnReclamo() throws Exception {
        long id = registrarYObtenerId(reclamo());

        mvc.perform(patch(ADMIN_RECLAMOS + "/" + id + "/en-atencion").with(admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado", is("EN_ATENCION")));
        mvc.perform(patch(ADMIN_RECLAMOS + "/" + id + "/en-atencion").with(admin()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo", is("ESTADO_INVALIDO")));
    }

    @Test
    @DisplayName("CA-3-30 responder desde REGISTRADO o EN_ATENCION lo pasa a RESPONDIDO con la respuesta, la fecha y el autor")
    void responderUnReclamo() throws Exception {
        long directo = registrarYObtenerId(reclamo());
        long tomado = registrarYObtenerId(reclamo());
        mvc.perform(patch(ADMIN_RECLAMOS + "/" + tomado + "/en-atencion").with(admin())).andExpect(status().isOk());
        String respuesta = "Procedemos con el cambio por una casaca nueva.";

        mvc.perform(post(ADMIN_RECLAMOS + "/" + directo + "/respuesta").with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(Map.of("respuesta", respuesta)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado", is("RESPONDIDO")))
                .andExpect(jsonPath("$.data.respuesta", is(respuesta)))
                .andExpect(jsonPath("$.data.respondidoEn", is("2026-10-19T15:00:00Z")))
                .andExpect(jsonPath("$.data.respondidoPor", is(ID_ADMIN)));
        mvc.perform(post(ADMIN_RECLAMOS + "/" + tomado + "/respuesta").with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(Map.of("respuesta", respuesta)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado", is("RESPONDIDO")));
    }

    @Test
    @DisplayName("CA-3-30 al responder se publica ReclamoRespondido con el consumidor y la respuesta")
    void publicaReclamoRespondido() throws Exception {
        long id = registrarYObtenerId(reclamo());
        mvc.perform(post(ADMIN_RECLAMOS + "/" + id + "/respuesta").with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(Map.of("respuesta", "Procedemos con el cambio.")))).andExpect(status().isOk());

        List<String> eventos = eventosDelTipo("ReclamoRespondido");
        assertThat(eventos).hasSize(1);
        assertThat((String) JsonPath.read(eventos.get(0), "$.numero")).isEqualTo("RC-2026-000001");
        assertThat((String) JsonPath.read(eventos.get(0), "$.consumidor.nombre")).isEqualTo("Rosa Díaz");
        assertThat((String) JsonPath.read(eventos.get(0), "$.consumidor.email")).isEqualTo("rosa.diaz@correo.com");
        assertThat((String) JsonPath.read(eventos.get(0), "$.respuesta")).isEqualTo("Procedemos con el cambio.");
        assertThat((String) JsonPath.read(eventos.get(0), "$.respondidoEn")).startsWith("2026-10-19T15:00:00");
    }

    @Test
    @DisplayName("CA-3-30 un reclamo respondido ya no se modifica: ni responder otra vez ni tomarlo (409) ni eliminarlo")
    void respondidoEsInmutable() throws Exception {
        long id = registrarYObtenerId(reclamo());
        mvc.perform(post(ADMIN_RECLAMOS + "/" + id + "/respuesta").with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(Map.of("respuesta", "Primera respuesta oficial.")))).andExpect(status().isOk());

        mvc.perform(post(ADMIN_RECLAMOS + "/" + id + "/respuesta").with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(Map.of("respuesta", "Intento de cambiar la respuesta.")))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("ESTADO_INVALIDO")));
        mvc.perform(patch(ADMIN_RECLAMOS + "/" + id + "/en-atencion").with(admin())).andExpect(status().isConflict());
        mvc.perform(delete(ADMIN_RECLAMOS + "/" + id).with(admin())).andExpect(status().isMethodNotAllowed());
        mvc.perform(put(ADMIN_RECLAMOS + "/" + id).with(admin()).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(get(ADMIN_RECLAMOS + "/" + id).with(admin()))
                .andExpect(jsonPath("$.data.respuesta", is("Primera respuesta oficial.")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reclamo", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-3-30 la respuesta tiene de 10 a 3000 caracteres")
    void validaLaRespuesta() throws Exception {
        long id = registrarYObtenerId(reclamo());
        for (String respuesta : List.of("corta", "x".repeat(3001), "")) {
            mvc.perform(post(ADMIN_RECLAMOS + "/" + id + "/respuesta").with(admin()).contentType(APPLICATION_JSON)
                    .content(cuerpo(Map.of("respuesta", respuesta)))).andExpect(status().isBadRequest());
        }
        mvc.perform(get(ADMIN_RECLAMOS + "/" + id).with(admin())).andExpect(jsonPath("$.data.estado", is("REGISTRADO")));
    }

    @Test
    @DisplayName("CA-3-31 el OPERADOR ve la bandeja y el detalle pero no puede tomar ni responder (403)")
    void operadorSoloConsulta() throws Exception {
        long id = registrarYObtenerId(reclamo());

        mvc.perform(get(ADMIN_RECLAMOS).with(operador())).andExpect(status().isOk());
        mvc.perform(get(ADMIN_RECLAMOS + "/" + id).with(operador())).andExpect(status().isOk());
        mvc.perform(patch(ADMIN_RECLAMOS + "/" + id + "/en-atencion").with(operador()))
                .andExpect(status().isForbidden());
        mvc.perform(post(ADMIN_RECLAMOS + "/" + id + "/respuesta").with(operador()).contentType(APPLICATION_JSON)
                .content(cuerpo(Map.of("respuesta", "Respuesta del operador.")))).andExpect(status().isForbidden());
        mvc.perform(get(ADMIN_RECLAMOS + "/" + id).with(operador()))
                .andExpect(jsonPath("$.data.estado", is("REGISTRADO")));
    }

    @Test
    @DisplayName("CA-3-31 sin sesión no se puede tomar ni responder (401)")
    void sinSesionNoSePuede() throws Exception {
        mvc.perform(patch(ADMIN_RECLAMOS + "/1/en-atencion")).andExpect(status().isUnauthorized());
        mvc.perform(post(ADMIN_RECLAMOS + "/1/respuesta").contentType(APPLICATION_JSON)
                .content(cuerpo(Map.of("respuesta", "Respuesta sin sesión.")))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CA-3-30 tomar o responder un reclamo que no existe responde 404")
    void reclamoInexistente() throws Exception {
        mvc.perform(patch(ADMIN_RECLAMOS + "/999999/en-atencion").with(admin())).andExpect(status().isNotFound());
        mvc.perform(post(ADMIN_RECLAMOS + "/999999/respuesta").with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(Map.of("respuesta", "Respuesta a la nada.")))).andExpect(status().isNotFound());
    }
}
