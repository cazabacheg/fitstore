package com.fitstore.payment.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.provider.simulado.SimuladoProvider;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Reglas del proveedor simulado (P2): el último tramo del token {@code sim_<últimos 4>} decide el resultado. */
class SimuladoProviderTest {

    private final SimuladoProvider proveedor = new SimuladoProvider();

    private static SolicitudCargo cargo(String token) {
        return new SolicitudCargo(UUID.randomUUID(), UUID.randomUUID().toString(), "FS-000124", new BigDecimal("59.90"),
                token, Medio.TARJETA, "ana@correo.pe", UUID.randomUUID().toString(), null);
    }

    @Test
    @DisplayName("CA-5-25 la tarjeta 4111 1111 1111 1111 (sim_1111) se aprueba")
    void apruebaLa1111() {
        ResultadoCargo r = proveedor.crearCargo(cargo("sim_1111"));

        assertThat(r.estado()).isEqualTo(EstadoTransaccion.APROBADA);
        assertThat(r.idProveedor()).isNotBlank();
        assertThat(r.descripcionMedio()).contains("1111");
    }

    @Test
    @DisplayName("CA-5-25 la tarjeta 4000 0000 0000 0002 (sim_0002) se rechaza por fondos insuficientes")
    void rechazaLa0002() {
        ResultadoCargo r = proveedor.crearCargo(cargo("sim_0002"));

        assertThat(r.estado()).isEqualTo(EstadoTransaccion.RECHAZADA);
        assertThat(r.mensajeLegible()).isEqualTo("Fondos insuficientes");
    }

    @Test
    @DisplayName("CA-5-25 la tarjeta 4000 0000 0000 0119 (sim_0119) queda PROCESANDO y se aprueba en la siguiente consulta")
    void procesandoLa0119() {
        ResultadoCargo r = proveedor.crearCargo(cargo("sim_0119"));

        assertThat(r.estado()).isEqualTo(EstadoTransaccion.PROCESANDO);
        assertThat(proveedor.consultarEstado(r.idProveedor()).estado()).isEqualTo(EstadoTransaccion.APROBADA);
    }

    @Test
    @DisplayName("CA-5-25 cualquier otro número se aprueba")
    void otroNumeroSeAprueba() {
        assertThat(proveedor.crearCargo(cargo("sim_4242")).estado()).isEqualTo(EstadoTransaccion.APROBADA);
    }

    @Test
    @DisplayName("CA-5-24 el proveedor simulado no necesita credenciales, no tiene clave pública y ofrece el medio tarjeta")
    void sinCredenciales() {
        assertThat(proveedor.proveedor()).isEqualTo(Proveedor.SIMULADO);
        assertThat(proveedor.tieneCredenciales()).isTrue();
        assertThat(proveedor.clavePublica()).isNull();
        assertThat(proveedor.medios()).containsExactly(Medio.TARJETA);
    }
}
