package com.fitstore.payment.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.api.FitstoreException;
import com.fitstore.payment.client.ConfiguracionTienda;
import com.fitstore.payment.client.SettingsClient;
import com.fitstore.payment.config.PropiedadesPagos;
import com.fitstore.payment.dto.ConfiguracionPublica;
import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.provider.mercadopago.MercadoPagoProvider;
import com.fitstore.payment.provider.simulado.SimuladoProvider;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Piezas del registro de proveedores que no necesitan Spring: circuito, registro y caché de la configuración de la tienda. */
class ProveedoresUnitariasTest {

    private static PropiedadesPagos propiedades(int ventana, int minimo, Duration espera) {
        return new PropiedadesPagos(new PropiedadesPagos.MercadoPago("pub", "token", "secreto", "http://localhost:9",
                Duration.ofSeconds(1)), new PropiedadesPagos.Simulado(true),
                new PropiedadesPagos.Yape(new BigDecimal("500.00")), new PropiedadesPagos.Verificador(Duration.ofMinutes(2),
                Duration.ofMinutes(2)), new PropiedadesPagos.Boleta("B001", "Leyenda", Duration.ofSeconds(20)),
                new PropiedadesPagos.Circuito(ventana, minimo, 50f, espera), Duration.ofMinutes(5));
    }

    // ------------------------------------------------------------ circuito

    @Test
    @DisplayName("CA-5-23 el circuito se abre tras fallos repetidos y deja de dejar pasar llamadas")
    void circuitoSeAbre() {
        CircuitoProveedores circuitos = new CircuitoProveedores(propiedades(2, 2, Duration.ofMinutes(1)));
        Runnable falla = () -> circuitos.ejecutar(Proveedor.MERCADO_PAGO, () -> {
            throw new PasarelaSinRespuestaException("sin respuesta", null);
        });

        assertThat(circuitos.estaAbierto(Proveedor.MERCADO_PAGO)).isFalse();
        for (int n = 0; n < 2; n++) {
            assertThatThrownBy(falla::run).isInstanceOf(PasarelaSinRespuestaException.class);
        }

        assertThat(circuitos.estaAbierto(Proveedor.MERCADO_PAGO)).isTrue();
        assertThatThrownBy(() -> circuitos.ejecutar(Proveedor.MERCADO_PAGO, () -> "no debería ejecutarse"))
                .isInstanceOf(ProveedorNoDisponibleException.class);
        // el circuito es por proveedor: el simulado sigue disponible
        assertThat(circuitos.estaAbierto(Proveedor.SIMULADO)).isFalse();
        assertThat(circuitos.ejecutar(Proveedor.SIMULADO, () -> "ok")).isEqualTo("ok");
    }

    @Test
    @DisplayName("CA-5-23 un pago rechazado o una consulta rechazada no cuentan como caída de la pasarela")
    void rechazosNoAbrenElCircuito() {
        CircuitoProveedores circuitos = new CircuitoProveedores(propiedades(2, 2, Duration.ofMinutes(1)));

        for (int n = 0; n < 5; n++) {
            assertThatThrownBy(() -> circuitos.ejecutar(Proveedor.MERCADO_PAGO, () -> {
                throw new ConsultaRechazadaException("no existe", null);
            })).isInstanceOf(ConsultaRechazadaException.class);
        }

        assertThat(circuitos.estaAbierto(Proveedor.MERCADO_PAGO)).isFalse();
    }

    // ------------------------------------------------------------ registro

    @Test
    @DisplayName("CA-5-03 un proveedor sin credenciales no se ofrece ni se puede usar para cobrar")
    void sinCredencialesNoSeOfrece() {
        PropiedadesPagos props = propiedades(10, 5, Duration.ofSeconds(30));
        MercadoPagoProvider sinCredenciales = new MercadoPagoProvider(
                new PropiedadesPagos.MercadoPago("", "", "", "http://localhost:9", Duration.ofSeconds(1)));
        ProveedorRegistry registro = new ProveedorRegistry(List.of(sinCredenciales, new SimuladoProvider()),
                new CircuitoProveedores(props), props);

        assertThat(registro.disponibles(null, new BigDecimal("100"))).extracting("proveedor").containsExactly(Proveedor.SIMULADO);
        assertThatThrownBy(() -> registro.validar(Proveedor.MERCADO_PAGO, Medio.TARJETA, new BigDecimal("100"), null))
                .isInstanceOf(FitstoreException.class).hasMessage("Ese medio de pago no está disponible");
        assertThat(registro.buscar(Proveedor.MERCADO_PAGO)).isPresent();
        assertThat(registro.buscar(Proveedor.CULQI)).isEmpty();
    }

    @Test
    @DisplayName("CA-5-03 el medio no soportado por el proveedor no se puede usar")
    void medioNoSoportado() {
        PropiedadesPagos props = propiedades(10, 5, Duration.ofSeconds(30));
        ProveedorRegistry registro = new ProveedorRegistry(List.of(new SimuladoProvider()), new CircuitoProveedores(props), props);

        assertThatThrownBy(() -> registro.validar(Proveedor.SIMULADO, Medio.YAPE, new BigDecimal("100"), null))
                .isInstanceOf(FitstoreException.class);
        assertThat(registro.validar(Proveedor.SIMULADO, Medio.TARJETA, new BigDecimal("100"), List.of("CULQI")))
                .isInstanceOf(SimuladoProvider.class);
    }

    // ------------------------------------------------------------ configuración de la tienda

    private static ConfiguracionPublica publica(String... metodos) {
        return new ConfiguracionPublica(new ConfiguracionPublica.Identidad("Moda", "Moda SAC", "20123456789", "Jr. Gamarra", null),
                new ConfiguracionPublica.Parametros(List.of(metodos)));
    }

    @Test
    @DisplayName("CA-5-03 la configuración de la tienda se cachea y se reutiliza la última copia si settings falla")
    void cacheDeLaTienda() {
        SettingsClient cliente = mock(SettingsClient.class);
        when(cliente.obtenerPublica()).thenReturn(ApiResponse.ok(publica("MERCADO_PAGO")));
        PropiedadesPagos props = new PropiedadesPagos(propiedades(10, 5, Duration.ofSeconds(30)).mercadopago(),
                new PropiedadesPagos.Simulado(true), new PropiedadesPagos.Yape(new BigDecimal("500.00")),
                new PropiedadesPagos.Verificador(Duration.ofMinutes(2), Duration.ofMinutes(2)),
                new PropiedadesPagos.Boleta("B001", "L", Duration.ofSeconds(20)),
                new PropiedadesPagos.Circuito(10, 5, 50f, Duration.ofSeconds(30)), Duration.ZERO);
        ConfiguracionTienda tienda = new ConfiguracionTienda(cliente, props);

        assertThat(tienda.metodosPago()).contains(List.of("MERCADO_PAGO"));
        when(cliente.obtenerPublica()).thenThrow(new IllegalStateException("settings caído"));
        // con TTL cero siempre vuelve a preguntar, pero si falla usa la última copia
        assertThat(tienda.publica()).isPresent();
        verify(cliente, times(2)).obtenerPublica();
    }

    @Test
    @DisplayName("CA-5-03 sin copia previa y con settings caído devuelve vacío para que quien lo usa aplique su plan B")
    void tiendaCaidaSinCopia() {
        SettingsClient cliente = mock(SettingsClient.class);
        when(cliente.obtenerPublica()).thenThrow(new IllegalStateException("settings caído"));
        ConfiguracionTienda tienda = new ConfiguracionTienda(cliente, propiedades(10, 5, Duration.ofSeconds(30)));

        assertThat(tienda.publica()).isEmpty();
        assertThat(tienda.metodosPago()).isEmpty();
    }

    @Test
    @DisplayName("CA-5-03 dentro del TTL no vuelve a consultar a settings")
    void dentroDelTtl() {
        SettingsClient cliente = mock(SettingsClient.class);
        when(cliente.obtenerPublica()).thenReturn(ApiResponse.ok(publica("MERCADO_PAGO", "YAPE")));
        ConfiguracionTienda tienda = new ConfiguracionTienda(cliente, propiedades(10, 5, Duration.ofSeconds(30)));

        tienda.publica();
        tienda.publica();
        assertThat(tienda.metodosPago()).contains(List.of("MERCADO_PAGO", "YAPE"));

        verify(cliente, times(1)).obtenerPublica();
    }
}
