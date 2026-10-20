package com.fitstore.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.common.persistence.JsonPersistenciaAutoConfiguration;
import com.fitstore.notification.model.EnvioCorreo;
import com.fitstore.notification.model.EstadoEnvio;
import com.fitstore.notification.pruebas.PostgresInicializador;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ContextConfiguration(initializers = PostgresInicializador.class)
@Import(JsonPersistenciaAutoConfiguration.class)
class EnvioCorreoRepositoryTest {

    private static final Instant AHORA = Instant.parse("2026-10-19T15:00:00Z");

    @Autowired
    EnvioCorreoRepository repositorio;

    @Autowired
    TestEntityManager em;

    private EnvioCorreo envio(String plantilla, String destinatario, String referencia, EstadoEnvio estado,
            Instant proximoIntento, Instant creadoEn) {
        EnvioCorreo e = new EnvioCorreo();
        e.setId(UUID.randomUUID());
        e.setEventoId(UUID.randomUUID());
        e.setPlantilla(plantilla);
        e.setDestinatario(destinatario);
        e.setAsunto("Asunto de " + plantilla);
        e.setVariables(Map.of("nombre", "Lucía", "codigoPedido", referencia));
        e.setEstado(estado);
        e.setIntentos(0);
        e.setHistorialIntentos(List.of());
        e.setProximoIntentoEn(proximoIntento);
        e.setReferencia(referencia);
        e.setCreadoEn(creadoEn);
        return e;
    }

    private EnvioCorreo guardar(EnvioCorreo e) {
        repositorio.saveAndFlush(e);
        em.clear();
        return e;
    }

    @Test
    @DisplayName("CA-0a-06 EnvioCorreoRepository inserta un envío con sus columnas jsonb")
    void insertaConJsonb() {
        EnvioCorreo e = envio("PEDIDO_CONFIRMADO", "lucia@correo.pe", "FS-000124", EstadoEnvio.PENDIENTE, AHORA, AHORA);
        e.setAdjunto("B001-00000001");
        guardar(e);

        EnvioCorreo leido = repositorio.findById(e.getId()).orElseThrow();
        assertThat(leido.getVariables()).containsEntry("nombre", "Lucía").containsEntry("codigoPedido", "FS-000124");
        assertThat(leido.getHistorialIntentos()).isEmpty();
        assertThat(leido.getAdjunto()).isEqualTo("B001-00000001");
        assertThat(leido.getEstado()).isEqualTo(EstadoEnvio.PENDIENTE);
    }

    @Test
    @DisplayName("CA-0a-06 EnvioCorreoRepository actualiza estado, intentos e historial")
    void actualizaEstadoIntentosEHistorial() {
        EnvioCorreo e = guardar(envio("BIENVENIDA", "ana@correo.pe", "FS-000001", EstadoEnvio.PENDIENTE, AHORA, AHORA));

        EnvioCorreo gestionado = repositorio.findById(e.getId()).orElseThrow();
        gestionado.setEstado(EstadoEnvio.FALLIDO);
        gestionado.setIntentos(4);
        gestionado.setUltimoError("Connection refused");
        gestionado.setHistorialIntentos(List.of(
                Map.of("numero", 1, "fecha", "2026-10-19T15:00:00Z", "error", "Connection refused")));
        repositorio.saveAndFlush(gestionado);
        em.clear();

        EnvioCorreo leido = repositorio.findById(e.getId()).orElseThrow();
        assertThat(leido.getEstado()).isEqualTo(EstadoEnvio.FALLIDO);
        assertThat(leido.getIntentos()).isEqualTo(4);
        assertThat(leido.getUltimoError()).isEqualTo("Connection refused");
        assertThat(leido.getHistorialIntentos()).hasSize(1);
        assertThat(leido.getHistorialIntentos().get(0)).containsEntry("numero", 1);
    }

    @Test
    @DisplayName("CA-0a-06 EnvioCorreoRepository lista solo los pendientes vencidos, del más antiguo al más nuevo")
    void listaPendientesVencidos() {
        guardar(envio("BIENVENIDA", "a@correo.pe", "R-1", EstadoEnvio.PENDIENTE, AHORA.minusSeconds(10), AHORA));
        guardar(envio("BIENVENIDA", "b@correo.pe", "R-2", EstadoEnvio.PENDIENTE, AHORA.minusSeconds(60), AHORA));
        guardar(envio("BIENVENIDA", "c@correo.pe", "R-3", EstadoEnvio.PENDIENTE, AHORA.plusSeconds(60), AHORA));
        guardar(envio("BIENVENIDA", "d@correo.pe", "R-4", EstadoEnvio.ENVIADO, AHORA.minusSeconds(60), AHORA));

        List<EnvioCorreo> pendientes = repositorio.tomarPendientes(AHORA, 10);

        assertThat(pendientes).extracting(EnvioCorreo::getReferencia).containsExactly("R-2", "R-1");
        assertThat(repositorio.tomarPendientes(AHORA, 1)).hasSize(1);
    }

    @Test
    @DisplayName("CA-0a-06 EnvioCorreoRepository filtra por estado, plantilla, destinatario, referencia y fechas")
    void filtraElRegistro() {
        guardar(envio("BIENVENIDA", "ana@correo.pe", "FS-000001", EstadoEnvio.ENVIADO, null, AHORA.minus(3, ChronoUnit.DAYS)));
        guardar(envio("PEDIDO_CONFIRMADO", "ana@correo.pe", "FS-000002", EstadoEnvio.FALLIDO, null, AHORA.minus(1, ChronoUnit.DAYS)));
        guardar(envio("PEDIDO_CONFIRMADO", "luis@correo.pe", "FS-000003", EstadoEnvio.ENVIADO, null, AHORA));
        var pagina = PageRequest.of(0, 10);

        assertThat(repositorio.filtrar(null, null, null, null, null, null, pagina).getTotalElements()).isEqualTo(3);
        assertThat(repositorio.filtrar(EstadoEnvio.FALLIDO, null, null, null, null, null, pagina).getContent())
                .extracting(EnvioCorreo::getReferencia).containsExactly("FS-000002");
        assertThat(repositorio.filtrar(null, "PEDIDO_CONFIRMADO", null, null, null, null, pagina).getTotalElements())
                .isEqualTo(2);
        assertThat(repositorio.filtrar(null, null, "ana@", null, null, null, pagina).getTotalElements()).isEqualTo(2);
        assertThat(repositorio.filtrar(null, null, null, "FS-000003", null, null, pagina).getContent())
                .extracting(EnvioCorreo::getDestinatario).containsExactly("luis@correo.pe");
        assertThat(repositorio.filtrar(null, null, null, null, AHORA.minus(2, ChronoUnit.DAYS), AHORA.minusSeconds(1),
                pagina).getContent()).extracting(EnvioCorreo::getReferencia).containsExactly("FS-000002");
        // más reciente primero
        assertThat(repositorio.filtrar(null, null, null, null, null, null, pagina).getContent())
                .extracting(EnvioCorreo::getReferencia).containsExactly("FS-000003", "FS-000002", "FS-000001");
    }

    @Test
    @DisplayName("CA-0a-06 EnvioCorreoRepository purga los envíos de más de 180 días y conserva el resto")
    void purgaPorAntiguedad() {
        Instant limite = AHORA.minus(180, ChronoUnit.DAYS);
        guardar(envio("BIENVENIDA", "a@correo.pe", "VIEJO", EstadoEnvio.ENVIADO, null, limite.minusSeconds(1)));
        guardar(envio("BIENVENIDA", "b@correo.pe", "LIMITE", EstadoEnvio.ENVIADO, null, limite));
        guardar(envio("BIENVENIDA", "c@correo.pe", "NUEVO", EstadoEnvio.ENVIADO, null, AHORA));

        int borrados = repositorio.purgarAnterioresA(limite);
        em.clear();

        assertThat(borrados).isEqualTo(1);
        assertThat(repositorio.findAll()).extracting(EnvioCorreo::getReferencia).containsExactlyInAnyOrder("LIMITE", "NUEVO");
    }

    @Test
    @DisplayName("CA-0a-06 EnvioCorreoRepository rechaza un estado fuera del CHECK")
    void rechazaEstadoInvalido() {
        guardar(envio("BIENVENIDA", "a@correo.pe", "R-1", EstadoEnvio.PENDIENTE, AHORA, AHORA));
        var jdbc = em.getEntityManager();

        assertThatThrownBy(() -> {
            jdbc.createNativeQuery("update envio_correo set estado = 'DESCONOCIDO'").executeUpdate();
            jdbc.flush();
        }).hasMessageContaining("ck_envio_correo_estado");
    }

    @Test
    @DisplayName("CA-0a-06 EnvioCorreoRepository rechaza una plantilla inexistente (FK)")
    void rechazaPlantillaInexistente() {
        EnvioCorreo e = envio("NO_EXISTE", "a@correo.pe", "R-1", EstadoEnvio.PENDIENTE, AHORA, AHORA);

        assertThatThrownBy(() -> repositorio.saveAndFlush(e)).hasMessageContaining("fk_envio_correo_plantilla");
    }

    @Test
    @DisplayName("CA-0a-06 EnvioCorreoRepository elimina un envío")
    void eliminaUnEnvio() {
        EnvioCorreo e = guardar(envio("BIENVENIDA", "a@correo.pe", "R-1", EstadoEnvio.PENDIENTE, AHORA, AHORA));

        repositorio.deleteById(e.getId());
        repositorio.flush();

        assertThat(repositorio.findById(e.getId())).isEmpty();
    }
}
