package com.fitstore.settings.reclamaciones.service.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Los 15 días hábiles del Libro de Reclamaciones cuentan de lunes a viernes, sin feriados (CA-3-25). */
class CalculadoraDiasHabilesTest {

    @Test
    @DisplayName("CA-3-25 15 días hábiles desde el lunes 19-oct-2026 vencen el lunes 9-nov-2026")
    void quinceDiasHabiles() {
        assertThat(CalculadoraDiasHabiles.sumar(LocalDate.of(2026, 10, 19), 15)).isEqualTo(LocalDate.of(2026, 11, 9));
    }

    @Test
    @DisplayName("CA-3-25 cinco días hábiles desde un lunes caen en el lunes siguiente")
    void cincoDesdeUnLunes() {
        assertThat(CalculadoraDiasHabiles.sumar(LocalDate.of(2026, 10, 19), 5)).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    @DisplayName("CA-3-25 desde un viernes, el primer día hábil es el lunes")
    void desdeUnViernes() {
        assertThat(CalculadoraDiasHabiles.sumar(LocalDate.of(2026, 10, 23), 1)).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    @DisplayName("CA-3-25 desde un sábado o un domingo se cuenta desde el lunes siguiente")
    void desdeFinDeSemana() {
        assertThat(CalculadoraDiasHabiles.sumar(LocalDate.of(2026, 10, 24), 1)).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(CalculadoraDiasHabiles.sumar(LocalDate.of(2026, 10, 25), 1)).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    @DisplayName("CA-3-25 cruza el fin de año")
    void cruzaFinDeAnio() {
        assertThat(CalculadoraDiasHabiles.sumar(LocalDate.of(2026, 12, 24), 15)).isEqualTo(LocalDate.of(2027, 1, 14));
    }

    @Test
    @DisplayName("CA-3-25 cero días deja la misma fecha")
    void cero() {
        assertThat(CalculadoraDiasHabiles.sumar(LocalDate.of(2026, 10, 19), 0)).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    @DisplayName("CA-3-25 el resultado nunca cae en sábado ni domingo, empiece donde empiece")
    void nuncaEnFinDeSemana() {
        LocalDate dia = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < 400; i++) {
            LocalDate limite = CalculadoraDiasHabiles.sumar(dia.plusDays(i), 15);
            assertThat(limite.getDayOfWeek()).isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        }
    }
}
