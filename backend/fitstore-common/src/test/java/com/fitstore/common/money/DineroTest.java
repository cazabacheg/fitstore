package com.fitstore.common.money;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DineroTest {

    @Test
    @DisplayName("Dinero redondea a 2 decimales half-up")
    void redondea() {
        assertThat(Dinero.redondear(new BigDecimal("10.005"))).isEqualByComparingTo("10.01");
        assertThat(Dinero.redondear(new BigDecimal("10.004"))).isEqualByComparingTo("10.00");
        assertThat(Dinero.redondear(new BigDecimal("10.005")).scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Dinero suma, resta y multiplica con escala 2")
    void operaciones() {
        assertThat(Dinero.sumar(new BigDecimal("59.90"), new BigDecimal("0.10"))).isEqualByComparingTo("60.00");
        assertThat(Dinero.restar(new BigDecimal("59.90"), new BigDecimal("9.90"))).isEqualByComparingTo("50.00");
        assertThat(Dinero.multiplicar(new BigDecimal("19.99"), 3)).isEqualByComparingTo("59.97");
        assertThat(Dinero.multiplicar(new BigDecimal("19.99"), 3).scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Dinero calcula un porcentaje redondeado half-up")
    void porcentaje() {
        assertThat(Dinero.porcentaje(new BigDecimal("59.90"), new BigDecimal("10"))).isEqualByComparingTo("5.99");
        assertThat(Dinero.porcentaje(new BigDecimal("33.33"), new BigDecimal("15"))).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("Dinero.de acepta texto y CERO tiene escala 2")
    void creacion() {
        assertThat(Dinero.de("12.5")).isEqualByComparingTo("12.50");
        assertThat(Dinero.de("12.5").scale()).isEqualTo(2);
        assertThat(Dinero.CERO.scale()).isEqualTo(2);
    }
}
