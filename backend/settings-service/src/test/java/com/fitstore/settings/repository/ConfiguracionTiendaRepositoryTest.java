package com.fitstore.settings.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.settings.configuracion.model.Beneficio;
import com.fitstore.settings.configuracion.model.ConfiguracionTienda;
import com.fitstore.settings.configuracion.model.MetodoPago;
import com.fitstore.settings.configuracion.model.ModalidadEntrega;
import com.fitstore.settings.configuracion.repository.ConfiguracionTiendaRepository;
import com.fitstore.settings.pruebas.Datos;
import com.fitstore.settings.pruebas.PruebaDatos;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class ConfiguracionTiendaRepositoryTest extends PruebaDatos {

    @Autowired ConfiguracionTiendaRepository repositorio;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void vaciar() {
        // La prueba usa su propia fila: no depende de la semilla (se revierte al terminar cada prueba)
        jdbc.update("DELETE FROM configuracion_tienda");
    }

    private ConfiguracionTienda guardada() {
        ConfiguracionTienda c = repositorio.saveAndFlush(Datos.configuracion());
        em.clear();
        return c;
    }

    @Test
    @DisplayName("CA-0a-06 inserta la fila única de configuración y la lee con todas sus secciones")
    void insertaYLee() {
        guardada();

        ConfiguracionTienda c = repositorio.findById(1).orElseThrow();
        assertThat(c.getIdentidad().getRuc()).isEqualTo("20123456789");
        assertThat(c.getTema().getColorPrimario()).isEqualTo("#C9B28C");
        assertThat(c.getContenido().getBeneficios()).containsExactly(new Beneficio("🚚", "Envíos a todo el Perú"));
        assertThat(c.getParametros().getMetodosPago()).containsExactlyInAnyOrder(MetodoPago.CULQI, MetodoPago.YAPE);
        assertThat(c.getParametros().getModalidadesEntrega()).containsExactly(ModalidadEntrega.DELIVERY_LOCAL);
        assertThat(c.getParametros().getCorreosAlertas()).containsExactly("alertas@tienda.pe");
        assertThat(c.getParametros().getDiasReembolsoPostventa()).isEqualTo(30);
        assertThat(c.getVersion()).isZero();
    }

    @Test
    @DisplayName("CA-0a-06 actualiza cada sección y la versión sube")
    void actualizaCadaSeccion() {
        guardada();

        ConfiguracionTienda c = repositorio.findById(1).orElseThrow();
        c.getIdentidad().setNombre("Otro nombre");
        c.getTema().setColorAcento("#112233");
        c.getContenido().setHeroTitulo("Nuevo título");
        c.getContenido().setBeneficios(List.of());
        c.getParametros().setExpiracionPedidoMinutos(45);
        c.getParametros().setMetodosPago(Set.of(MetodoPago.MERCADO_PAGO));
        repositorio.saveAndFlush(c);
        em.clear();

        ConfiguracionTienda leida = repositorio.findById(1).orElseThrow();
        assertThat(leida.getIdentidad().getNombre()).isEqualTo("Otro nombre");
        assertThat(leida.getTema().getColorAcento()).isEqualTo("#112233");
        assertThat(leida.getContenido().getHeroTitulo()).isEqualTo("Nuevo título");
        assertThat(leida.getContenido().getBeneficios()).isEmpty();
        assertThat(leida.getParametros().getExpiracionPedidoMinutos()).isEqualTo(45);
        assertThat(leida.getParametros().getMetodosPago()).containsExactly(MetodoPago.MERCADO_PAGO);
        assertThat(leida.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK (id = 1): no admite una segunda fila")
    void soloUnaFila() {
        ConfiguracionTienda otra = Datos.configuracion();
        otra.setId(2);

        assertThatThrownBy(() -> repositorio.saveAndFlush(otra)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 bloqueo optimista: guardar con una versión desactualizada falla")
    void bloqueoOptimista() {
        guardada();
        ConfiguracionTienda c = repositorio.findById(1).orElseThrow();
        // Otro administrador guarda antes (sube la versión en la base)
        jdbc.update("UPDATE configuracion_tienda SET version = version + 1 WHERE id = 1");

        c.getIdentidad().setNombre("Mi cambio tardío");

        assertThatThrownBy(() -> repositorio.saveAndFlush(c)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK de colores: solo #RRGGBB")
    void colorInvalido() {
        ConfiguracionTienda c = Datos.configuracion();
        c.getTema().setColorFondo("rojo");

        assertThatThrownBy(() -> repositorio.saveAndFlush(c)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK de RUC: 11 dígitos")
    void rucInvalido() {
        ConfiguracionTienda c = Datos.configuracion();
        c.getIdentidad().setRuc("2012345678");

        assertThatThrownBy(() -> repositorio.saveAndFlush(c)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK de expiración del pedido: de 10 a 120 minutos")
    void expiracionFueraDeRango() {
        for (int minutos : new int[] {9, 121}) {
            jdbc.update("DELETE FROM configuracion_tienda");
            ConfiguracionTienda c = Datos.configuracion();
            c.getParametros().setExpiracionPedidoMinutos(minutos);
            assertThatThrownBy(() -> repositorio.saveAndFlush(c)).isInstanceOf(DataIntegrityViolationException.class);
            em.clear();
        }
    }

    @Test
    @DisplayName("CA-0a-06 CHECK de días de reembolso: de 0 a 365")
    void diasReembolsoFueraDeRango() {
        ConfiguracionTienda c = Datos.configuracion();
        c.getParametros().setDiasReembolsoPostventa(366);

        assertThatThrownBy(() -> repositorio.saveAndFlush(c)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK: al menos un método de pago y una modalidad de entrega")
    void sinMetodosOModalidades() {
        ConfiguracionTienda sinPago = Datos.configuracion();
        sinPago.getParametros().setMetodosPago(Set.of());
        assertThatThrownBy(() -> repositorio.saveAndFlush(sinPago)).isInstanceOf(DataIntegrityViolationException.class);
        em.clear();

        ConfiguracionTienda sinEntrega = Datos.configuracion();
        sinEntrega.getParametros().setModalidadesEntrega(Set.of());
        assertThatThrownBy(() -> repositorio.saveAndFlush(sinEntrega)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK de correos de alertas: de 1 a 5")
    void correosDeAlertasFueraDeRango() {
        ConfiguracionTienda ninguno = Datos.configuracion();
        ninguno.getParametros().setCorreosAlertas(List.of());
        assertThatThrownBy(() -> repositorio.saveAndFlush(ninguno)).isInstanceOf(DataIntegrityViolationException.class);
        em.clear();

        ConfiguracionTienda seis = Datos.configuracion();
        seis.getParametros().setCorreosAlertas(List.of("a@x.pe", "b@x.pe", "c@x.pe", "d@x.pe", "e@x.pe", "f@x.pe"));
        assertThatThrownBy(() -> repositorio.saveAndFlush(seis)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK de beneficios: máximo 3")
    void masDeTresBeneficios() {
        ConfiguracionTienda c = Datos.configuracion();
        c.getContenido().setBeneficios(List.of(
                new Beneficio("1", "a"), new Beneficio("2", "b"), new Beneficio("3", "c"), new Beneficio("4", "d")));

        assertThatThrownBy(() -> repositorio.saveAndFlush(c)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
