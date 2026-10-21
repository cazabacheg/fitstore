package com.fitstore.notification.plantillas;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.notification.model.Plantilla;
import com.fitstore.notification.pruebas.PostgresInicializador;
import com.fitstore.notification.repository.PlantillaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Las 18 plantillas con sus textos predeterminados (los sembrados en {@code V2__plantillas.sql}) renderizan con sus
 * datos de ejemplo, con la marca de la tienda, en HTML y texto plano (SPEC-7 CA-7-15 y CA-7-18).
 */
@SpringBootTest
@ActiveProfiles({"test", "sin-config-server"})
@ContextConfiguration(initializers = PostgresInicializador.class)
class RenderizadorCorreoTest {

    private static final MarcaVista MARCA = new MarcaVista("FitStore Gamarra", "http://tienda.test/media/logo.png",
            "#C9B28C", "#2B2621", "#FAF9F7", "#26221D", "ventas@fitstore.test", "987654321",
            "Lunes a sábado de 9:00 a 19:00", "Galería Gamarra, Jr. Gamarra 700, tienda 210",
            "https://instagram.com/fitstore", "https://facebook.com/fitstore", "https://tiktok.com/@fitstore",
            "http://tienda.test");

    @Autowired RenderizadorCorreo renderizador;
    @Autowired PlantillaRepository plantillas;

    private CorreoRenderizado render(CatalogoPlantillas catalogo, MarcaVista marca, Map<String, Object> variables) {
        Plantilla plantilla = plantillas.findById(catalogo.name()).orElseThrow();
        return renderizador.renderizar(catalogo, TextosPlantilla.de(plantilla), variables, marca);
    }

    @Test
    @DisplayName("CA-7-15 el catálogo tiene las 18 plantillas (14 al cliente y 4 a la tienda) y todas están sembradas")
    void catalogoCompleto() {
        assertThat(CatalogoPlantillas.values()).hasSize(18);
        assertThat(java.util.Arrays.stream(CatalogoPlantillas.values())
                .filter(c -> c.para() == CatalogoPlantillas.Para.CLIENTE)).hasSize(14);
        assertThat(plantillas.findAll()).extracting(Plantilla::getCodigo)
                .containsExactlyInAnyOrderElementsOf(
                        java.util.Arrays.stream(CatalogoPlantillas.values()).map(Enum::name).toList());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(CatalogoPlantillas.class)
    @DisplayName("CA-7-15 cada plantilla renderiza con logo, color primario, nombre, pie, enlaces y versión en texto plano")
    void renderizaConElDisenoComun(CatalogoPlantillas catalogo) {
        CorreoRenderizado correo = render(catalogo, MARCA, EjemplosPlantilla.de(catalogo));

        assertThat(correo.asunto()).isNotBlank().doesNotContain("{{").doesNotContain("}}");
        assertThat(correo.html())
                .contains("<img").contains("http://tienda.test/media/logo.png")
                .contains("#C9B28C")
                .contains("FitStore Gamarra")
                .contains("Galería Gamarra, Jr. Gamarra 700, tienda 210")
                .contains("Lunes a sábado de 9:00 a 19:00")
                .contains("https://instagram.com/fitstore").contains("https://facebook.com/fitstore")
                .contains("http://tienda.test/politicas/terminos").contains("http://tienda.test/politicas/privacidad")
                .doesNotContain("{{").doesNotContain(" th:").doesNotContain("<th:");
        assertThat(correo.texto()).isNotBlank().doesNotContain("<").doesNotContain("{{")
                .contains("FitStore Gamarra").contains("Galería Gamarra, Jr. Gamarra 700, tienda 210");
    }

    @Test
    @DisplayName("CA-7-15 el texto sobre el color primario es blanco u oscuro, el que contraste más")
    void textoSobreElPrimario() {
        MarcaVista oscura = conPrimario("#1A1A1A");
        MarcaVista clara = conPrimario("#F5F0DC");

        String htmlOscuro = render(CatalogoPlantillas.BIENVENIDA, oscura, EjemplosPlantilla.de(CatalogoPlantillas.BIENVENIDA)).html();
        String htmlClaro = render(CatalogoPlantillas.BIENVENIDA, clara, EjemplosPlantilla.de(CatalogoPlantillas.BIENVENIDA)).html();

        assertThat(oscura.colorSobrePrimario()).isEqualTo("#FFFFFF");
        assertThat(clara.colorSobrePrimario()).isEqualTo("#26221D");
        assertThat(htmlOscuro).contains("background-color:#1A1A1A;color:#FFFFFF");
        assertThat(htmlClaro).contains("background-color:#F5F0DC;color:#26221D");
    }

    private static MarcaVista conPrimario(String color) {
        return new MarcaVista(MARCA.nombre(), MARCA.logoUrl(), color, MARCA.colorSecundario(), MARCA.colorFondo(),
                MARCA.colorTexto(), MARCA.email(), MARCA.telefono(), MARCA.horario(), MARCA.direccion(),
                MARCA.instagramUrl(), MARCA.facebookUrl(), MARCA.tiktokUrl(), MARCA.urlTienda());
    }

    @Test
    @DisplayName("CA-7-15 el botón sale solo si la plantilla tiene texto de botón y enlace")
    void botonConEnlace() {
        Map<String, Object> variables = new HashMap<>(EjemplosPlantilla.de(CatalogoPlantillas.RECUPERAR_PASSWORD));

        CorreoRenderizado conBoton = render(CatalogoPlantillas.RECUPERAR_PASSWORD, MARCA, variables);
        CorreoRenderizado sinBoton = render(CatalogoPlantillas.PASSWORD_CAMBIADO, MARCA,
                EjemplosPlantilla.de(CatalogoPlantillas.PASSWORD_CAMBIADO));

        assertThat(conBoton.html()).contains("Restablecer contraseña").contains(String.valueOf(variables.get("enlaceBoton")));
        assertThat(conBoton.texto()).contains(String.valueOf(variables.get("enlaceBoton")));
        assertThat(sinBoton.html()).doesNotContain("<a href=\"null\"");
    }

    @Test
    @DisplayName("CA-7-15 el mensaje admite saltos de línea y negrita, y todo lo demás se escapa")
    void formatoDelMensaje() {
        Map<String, Object> variables = new HashMap<>(EjemplosPlantilla.de(CatalogoPlantillas.BIENVENIDA));
        variables.put("nombre", "<script>alert(1)</script>");
        TextosPlantilla textos = new TextosPlantilla("Hola {{nombre}}", "Título <b>",
                "Primera línea con **negrita**\nSegunda línea {{nombre}} ${7*7}", null);

        CorreoRenderizado correo = renderizador.renderizar(CatalogoPlantillas.BIENVENIDA, textos, variables, MARCA);

        assertThat(correo.html()).contains("<strong>negrita</strong>").contains("<br")
                .contains("&lt;script&gt;alert(1)&lt;/script&gt;").contains("${7*7}")
                .doesNotContain("<script>").doesNotContain("<b>");
        assertThat(correo.asunto()).isEqualTo("Hola <script>alert(1)</script>");
        assertThat(correo.texto()).contains("negrita").doesNotContain("**");
    }

    @Test
    @DisplayName("CA-7-18 las fechas y los montos salen en formato es-PE")
    void formatoEsPe() {
        assertThat(FormatoEsPe.fecha(LocalDate.of(2026, 10, 22))).isEqualTo("jueves 22 de octubre");
        assertThat(FormatoEsPe.moneda(new BigDecimal("332.46"))).isEqualTo("S/ 332.46");
        assertThat(FormatoEsPe.moneda(new BigDecimal("1234.5"))).isEqualTo("S/ 1,234.50");
        assertThat(FormatoEsPe.moneda(BigDecimal.TEN)).isEqualTo("S/ 10.00");
    }

    @Test
    @DisplayName("CA-7-18 ningún correo lleva el DNI en el cuerpo")
    void sinDni() {
        for (CatalogoPlantillas catalogo : CatalogoPlantillas.values()) {
            Map<String, Object> variables = new HashMap<>(EjemplosPlantilla.de(catalogo));
            variables.put("dni", "70123499");
            variables.put("destinatarioDni", "70123499");

            CorreoRenderizado correo = render(catalogo, MARCA, variables);

            assertThat(correo.html()).as(catalogo.name()).doesNotContain("70123499");
            assertThat(correo.texto()).as(catalogo.name()).doesNotContain("70123499");
            assertThat(correo.asunto()).as(catalogo.name()).doesNotContain("70123499");
        }
        assertThat(CatalogoPlantillas.values()).allSatisfy(c -> assertThat(c.variablesPermitidas()).doesNotContain("dni"));
    }

    @Test
    @DisplayName("CA-7-18 el correo de confirmación muestra el resumen del pedido con montos es-PE")
    void confirmacionConResumen() {
        Map<String, Object> variables = EjemplosPlantilla.de(CatalogoPlantillas.PEDIDO_CONFIRMADO);

        CorreoRenderizado correo = render(CatalogoPlantillas.PEDIDO_CONFIRMADO, MARCA, variables);

        assertThat(correo.html()).contains("Legging Power").contains("S/ 332.46");
        assertThat(correo.texto()).contains("Legging Power").contains("S/ 332.46");
        assertThat(CatalogoPlantillas.PEDIDO_CONFIRMADO.variablesPermitidas())
                .contains("nombre", "codigoPedido", "total", "modalidad", "nombreTienda");
    }
}
