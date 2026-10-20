package com.fitstore.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.common.persistence.JsonPersistenciaAutoConfiguration;
import com.fitstore.notification.model.MarcaTienda;
import com.fitstore.notification.pruebas.PostgresInicializador;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ContextConfiguration(initializers = PostgresInicializador.class)
@Import(JsonPersistenciaAutoConfiguration.class)
class MarcaTiendaRepositoryTest {

    @Autowired
    MarcaTiendaRepository repositorio;

    @Autowired
    TestEntityManager em;

    @Test
    @DisplayName("CA-0a-06 MarcaTiendaRepository trae la fila semilla con la marca demo de FitStore")
    @SuppressWarnings("unchecked")
    void traeLaFilaSemilla() {
        MarcaTienda marca = repositorio.findById((short) 1).orElseThrow();

        assertThat(repositorio.count()).isEqualTo(1);
        assertThat(marca.getVersion()).isZero();
        assertThat(marca.getCorreosAlertas()).containsExactly("ventas@tienda.pe");
        Map<String, Object> config = marca.getConfiguracion();
        assertThat((Map<String, Object>) config.get("identidad")).containsEntry("nombre", "FitStore");
        assertThat((Map<String, Object>) config.get("tema")).containsEntry("colorPrimario", "#C9B28C")
                .containsEntry("colorSecundario", "#2B2621").containsEntry("colorAcento", "#4E5B45")
                .containsEntry("colorFondo", "#FAF9F7").containsEntry("colorTexto", "#26221D");
        Map<String, Object> contenido = (Map<String, Object>) config.get("contenido");
        assertThat(contenido).containsEntry("email", "ventas@tienda.pe").containsEntry("telefono", "987654321")
                .containsEntry("horario", "Lunes a sábado de 9:00 a 19:00");
        assertThat((Map<String, Object>) contenido.get("tiendaFisica"))
                .containsEntry("direccion", "Galería Gamarra, Jr. Gamarra 700, tienda 210");
    }

    @Test
    @DisplayName("CA-0a-06 MarcaTiendaRepository actualiza la copia desde ConfiguracionActualizada")
    void actualizaDesdeElEvento() {
        MarcaTienda marca = repositorio.findById((short) 1).orElseThrow();
        marca.setConfiguracion(Map.of("identidad", Map.of("nombre", "Gym Wear Gamarra")));
        marca.setCorreosAlertas(new String[] {"dueno@tienda.pe", "almacen@tienda.pe"});
        marca.setVersion(7);
        marca.setActualizadoEn(Instant.parse("2026-10-19T15:00:00Z"));
        repositorio.saveAndFlush(marca);
        em.clear();

        MarcaTienda leida = repositorio.findById((short) 1).orElseThrow();
        assertThat(leida.getVersion()).isEqualTo(7);
        assertThat(leida.getCorreosAlertas()).containsExactly("dueno@tienda.pe", "almacen@tienda.pe");
        assertThat(leida.getConfiguracion()).containsKey("identidad");
        assertThat(leida.getActualizadoEn()).isEqualTo(Instant.parse("2026-10-19T15:00:00Z"));
    }

    @Test
    @DisplayName("CA-0a-06 MarcaTiendaRepository solo admite la fila con id = 1 (CHECK)")
    void soloAdmiteUnaFila() {
        MarcaTienda otra = new MarcaTienda();
        otra.setId((short) 2);
        otra.setConfiguracion(Map.of());
        otra.setCorreosAlertas(new String[] {"x@tienda.pe"});
        otra.setVersion(1);
        otra.setActualizadoEn(Instant.now());

        assertThatThrownBy(() -> repositorio.saveAndFlush(otra)).hasMessageContaining("ck_marca_tienda_unica");
    }

    @Test
    @DisplayName("CA-0a-06 MarcaTiendaRepository no admite una segunda fila con id = 1 (PK)")
    void rechazaPkDuplicada() {
        var manager = em.getEntityManager();

        assertThatThrownBy(() -> manager.createNativeQuery(
                "insert into marca_tienda (id, configuracion, correos_alertas, version, actualizado_en) "
                        + "values (1, '{}', '{}', 1, now())").executeUpdate())
                .hasMessageContaining("marca_tienda_pkey");
    }
}
