package com.fitstore.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.notification.model.EnvioCorreo;
import com.fitstore.notification.model.EstadoEnvio;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.CorreoRenderizado;
import com.fitstore.notification.plantillas.MarcaService;
import com.fitstore.notification.plantillas.RenderizadorCorreo;
import com.fitstore.notification.plantillas.TextosPlantilla;
import com.fitstore.notification.repository.EnvioCorreoRepository;
import com.fitstore.notification.repository.PlantillaRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Datos de demostración (perfiles dev y demo, SPEC-7 sección 6): base propia con las migraciones de {@code db/dev}.
 * El registro de envíos queda alineado con los pedidos FS-000101 a FS-000111 de {@code docs/demo/ids-demo.md}.
 */
@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/dev")
@ActiveProfiles({"test", "sin-config-server"})
class DatosDemoTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.10");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired EnvioCorreoRepository envios;
    @Autowired PlantillaRepository plantillas;
    @Autowired RenderizadorCorreo renderizador;
    @Autowired MarcaService marca;

    @Test
    @DisplayName("la demo deja el registro con correos enviados, uno fallido y uno pendiente")
    void registroDeLaDemo() {
        List<EnvioCorreo> todos = envios.findAll();

        assertThat(todos).hasSizeGreaterThanOrEqualTo(20);
        Map<EstadoEnvio, Long> porEstado = todos.stream()
                .collect(Collectors.groupingBy(EnvioCorreo::getEstado, Collectors.counting()));
        assertThat(porEstado.get(EstadoEnvio.FALLIDO)).isEqualTo(1);
        assertThat(porEstado.get(EstadoEnvio.PENDIENTE)).isEqualTo(1);
        assertThat(porEstado.get(EstadoEnvio.ENVIADO)).isGreaterThan(15);
        EnvioCorreo fallido = todos.stream().filter(e -> e.getEstado() == EstadoEnvio.FALLIDO).findFirst().orElseThrow();
        assertThat(fallido.getUltimoError()).isNotBlank();
        assertThat(fallido.getHistorialIntentos()).hasSize(5);
        assertThat(fallido.getProximoIntentoEn()).isNull();
    }

    @Test
    @DisplayName("los correos de pedido de la demo son de los pedidos FS-000101 a FS-000111 y nunca de uno sin pago")
    void referenciasAlineadasConLosPedidos() {
        List<String> referencias = envios.findAll().stream().map(EnvioCorreo::getReferencia)
                .filter(r -> r != null && r.startsWith("FS-")).distinct().toList();

        assertThat(referencias).isNotEmpty().allMatch(r -> r.compareTo("FS-000101") >= 0 && r.compareTo("FS-000111") <= 0);
        // FS-000105 (por pagar) y FS-000111 (expirado) nunca pagaron: antes del pago no se envía nada
        assertThat(referencias).doesNotContain("FS-000105", "FS-000111");
        assertThat(envios.findAll()).allMatch(e -> e.getAdjunto() == null);
    }

    @Test
    @DisplayName("cada correo de la demo se puede volver a renderizar (reenviar) con la marca y los textos vigentes")
    void todosRenderizan() {
        for (EnvioCorreo envio : envios.findAll()) {
            var plantilla = plantillas.findById(envio.getPlantilla()).orElseThrow();

            CorreoRenderizado correo = renderizador.renderizar(CatalogoPlantillas.valueOf(envio.getPlantilla()),
                    TextosPlantilla.de(plantilla), envio.getVariables(), marca.actual());

            assertThat(correo.asunto()).as(envio.getAsunto()).isNotBlank().doesNotContain("{{");
            assertThat(correo.html()).as(envio.getAsunto()).contains(envio.getVariables().get("nombre") != null
                    ? String.valueOf(envio.getVariables().get("nombre")) : "FitStore");
        }
    }
}
