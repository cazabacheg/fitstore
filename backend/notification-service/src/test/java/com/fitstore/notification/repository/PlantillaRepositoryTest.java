package com.fitstore.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.notification.model.Plantilla;
import com.fitstore.notification.pruebas.PostgresInicializador;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles({"test", "sin-config-server"})
@ContextConfiguration(initializers = PostgresInicializador.class)
class PlantillaRepositoryTest {

    private static final Set<String> CODIGOS = Set.of(
            "BIENVENIDA", "RECUPERAR_PASSWORD", "INVITACION_STAFF", "PASSWORD_CAMBIADO", "PEDIDO_CONFIRMADO",
            "PEDIDO_CANCELADO", "REEMBOLSO_REALIZADO", "EN_CAMINO", "ENTREGA_FALLIDA", "LISTO_PARA_RECOGER",
            "ENTREGADO", "RECLAMO_COPIA", "RECLAMO_RESPUESTA", "PEDIDO_DEVUELTO", "ALERTA_STOCK_BAJO",
            "ALERTA_SIN_STOCK", "ALERTA_REEMBOLSO_FALLIDO", "ALERTA_RECLAMO_NUEVO");

    private static final Set<String> OBLIGATORIAS = Set.of(
            "RECUPERAR_PASSWORD", "INVITACION_STAFF", "PEDIDO_CONFIRMADO", "RECLAMO_COPIA", "RECLAMO_RESPUESTA");

    @Autowired
    PlantillaRepository repositorio;

    @Autowired
    TestEntityManager em;

    @Test
    @DisplayName("CA-0a-06 PlantillaRepository lista las 18 plantillas sembradas, activas y con las obligatorias marcadas")
    void listaLaSemilla() {
        List<Plantilla> todas = repositorio.findAll();

        assertThat(todas).extracting(Plantilla::getCodigo).containsExactlyInAnyOrderElementsOf(CODIGOS);
        assertThat(todas).allSatisfy(p -> {
            assertThat(p.isActiva()).isTrue();
            assertThat(p.getAsunto()).isNotBlank();
            assertThat(p.getTitulo()).isNotBlank();
            assertThat(p.getMensaje()).isNotBlank();
        });
        assertThat(todas).filteredOn(Plantilla::isObligatoria).extracting(Plantilla::getCodigo)
                .containsExactlyInAnyOrderElementsOf(OBLIGATORIAS);
    }

    @Test
    @DisplayName("CA-0a-06 PlantillaRepository actualiza los textos, el botón y el estado activa")
    void actualizaTextosYActiva() {
        Plantilla p = repositorio.findById("BIENVENIDA").orElseThrow();
        p.setAsunto("Hola {{nombre}}, ya eres parte de {{nombreTienda}}");
        p.setTitulo("Nos alegra tenerte");
        p.setMensaje("Gracias por registrarte.\n**Ya puedes comprar.**");
        p.setTextoBoton("Ir a la tienda");
        p.setActiva(false);
        p.setActualizadaEn(Instant.parse("2026-10-19T15:00:00Z"));
        p.setActualizadaPor("admin@fitstore.local");
        repositorio.saveAndFlush(p);
        em.clear();

        Plantilla leida = repositorio.findById("BIENVENIDA").orElseThrow();
        assertThat(leida.getAsunto()).startsWith("Hola {{nombre}}");
        assertThat(leida.getMensaje()).contains("\n**Ya puedes comprar.**");
        assertThat(leida.getTextoBoton()).isEqualTo("Ir a la tienda");
        assertThat(leida.isActiva()).isFalse();
        assertThat(leida.getActualizadaPor()).isEqualTo("admin@fitstore.local");
    }

    @Test
    @DisplayName("CA-0a-06 PlantillaRepository inserta y elimina una plantilla sin envíos")
    void insertaYElimina() {
        Plantilla nueva = new Plantilla();
        nueva.setCodigo("PLANTILLA_DE_PRUEBA");
        nueva.setAsunto("a");
        nueva.setTitulo("t");
        nueva.setMensaje("m");
        nueva.setActiva(true);
        nueva.setObligatoria(false);
        nueva.setActualizadaEn(Instant.now());
        repositorio.saveAndFlush(nueva);
        assertThat(repositorio.existsById("PLANTILLA_DE_PRUEBA")).isTrue();

        repositorio.deleteById("PLANTILLA_DE_PRUEBA");
        repositorio.flush();

        assertThat(repositorio.existsById("PLANTILLA_DE_PRUEBA")).isFalse();
    }

    @Test
    @DisplayName("CA-0a-06 PlantillaRepository rechaza un código duplicado (PK)")
    void rechazaCodigoDuplicado() {
        var manager = em.getEntityManager();

        assertThatThrownBy(() -> manager.createNativeQuery(
                "insert into plantilla (codigo, asunto, titulo, mensaje, activa, obligatoria, actualizada_en) "
                        + "values ('BIENVENIDA', 'a', 't', 'm', true, false, now())").executeUpdate())
                .hasMessageContaining("plantilla_pkey");
    }

    @Test
    @DisplayName("CA-0a-06 PlantillaRepository no deja eliminar una plantilla con envíos (FK)")
    void rechazaEliminarConEnvios() {
        var manager = em.getEntityManager();
        manager.createNativeQuery("insert into envio_correo (id, plantilla, destinatario, asunto, variables, estado, "
                + "intentos, historial_intentos, creado_en) values (gen_random_uuid(), 'BIENVENIDA', 'a@correo.pe', "
                + "'a', '{}', 'PENDIENTE', 0, '[]', now())").executeUpdate();

        assertThatThrownBy(() -> {
            manager.createNativeQuery("delete from plantilla where codigo = 'BIENVENIDA'").executeUpdate();
        }).hasMessageContaining("fk_envio_correo_plantilla");
    }
}
