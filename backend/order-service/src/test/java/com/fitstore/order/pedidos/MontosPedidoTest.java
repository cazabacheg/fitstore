package com.fitstore.order.pedidos;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.order.pedidos.service.MontosPedido;
import com.fitstore.order.pedidos.service.MontosPedido.Item;
import com.fitstore.order.pedidos.service.MontosPedido.Montos;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Montos del pedido (D8): PEN con IGV, 2 decimales, redondeo half-up. */
class MontosPedidoTest {

    private static BigDecimal d(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    @DisplayName("CA-4-25 total = subtotal − descuentoTotal + costoEnvio y cada ítem cuadra igual")
    void totalCuadra() {
        List<Item> items = List.of(new Item(d("59.90"), 2, d("11.98")), new Item(d("39.90"), 1, d("0")));

        Montos montos = MontosPedido.calcular(items, d("12.00"));

        assertThat(montos.subtotal()).isEqualByComparingTo("159.70");
        assertThat(montos.descuentoTotal()).isEqualByComparingTo("11.98");
        assertThat(montos.costoEnvio()).isEqualByComparingTo("12.00");
        assertThat(montos.total()).isEqualByComparingTo("159.72");
        assertThat(montos.total()).isEqualByComparingTo(
                montos.subtotal().subtract(montos.descuentoTotal()).add(montos.costoEnvio()));
        assertThat(MontosPedido.totalItem(items.get(0))).isEqualByComparingTo("107.82");
        assertThat(MontosPedido.totalItem(items.get(1))).isEqualByComparingTo("39.90");
    }

    @Test
    @DisplayName("CA-4-25 el descuento de un ítem se redondea half-up a 2 decimales")
    void redondeoHalfUp() {
        // 33.33 × 3 × 15 % = 14.9985 → 15.00; 10.05 × 1 × 5 % = 0.5025 → 0.50; 0.10 × 5 × 5 % = 0.025 → 0.03
        assertThat(MontosPedido.descuento(d("33.33"), 3, d("15"))).isEqualByComparingTo("15.00");
        assertThat(MontosPedido.descuento(d("10.05"), 1, d("5"))).isEqualByComparingTo("0.50");
        assertThat(MontosPedido.descuento(d("0.10"), 5, d("5"))).isEqualByComparingTo("0.03");
        assertThat(MontosPedido.descuento(d("59.90"), 2, d("0"))).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("CA-4-25 los montos salen con 2 decimales aunque el envío sea cero (recojo)")
    void dosDecimales() {
        Montos montos = MontosPedido.calcular(List.of(new Item(d("50"), 1, d("0"))), BigDecimal.ZERO);

        assertThat(montos.total().scale()).isEqualTo(2);
        assertThat(montos.costoEnvio().scale()).isEqualTo(2);
        assertThat(montos.total()).isEqualByComparingTo("50.00");
    }
}
