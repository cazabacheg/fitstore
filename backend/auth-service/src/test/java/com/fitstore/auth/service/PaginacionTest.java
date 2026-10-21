package com.fitstore.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Parámetros {@code page}, {@code size} y {@code sort} de los listados del backoffice (contracts K6). */
class PaginacionTest {

    private static final Set<String> PERMITIDOS = Set.of("email", "creadoEn");
    private static final Sort POR_DEFECTO = Sort.by(Sort.Direction.DESC, "creadoEn");

    @Test
    @DisplayName("CA-1-32 sin parámetros usa la página 0, 20 elementos y el orden por defecto")
    void valoresPorDefecto() {
        Pageable p = Paginacion.de(null, null, null, PERMITIDOS, POR_DEFECTO);

        assertThat(p.getPageNumber()).isZero();
        assertThat(p.getPageSize()).isEqualTo(20);
        assertThat(p.getSort()).isEqualTo(POR_DEFECTO);
    }

    @Test
    @DisplayName("CA-1-32 el tamaño se limita a 100, nunca baja de 1 y la página nunca es negativa")
    void limites() {
        assertThat(Paginacion.de(-3, 5000, null, PERMITIDOS, POR_DEFECTO).getPageSize()).isEqualTo(100);
        assertThat(Paginacion.de(-3, 5000, null, PERMITIDOS, POR_DEFECTO).getPageNumber()).isZero();
        assertThat(Paginacion.de(0, 0, null, PERMITIDOS, POR_DEFECTO).getPageSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-1-32 une 'campo' y 'asc|desc' aunque Spring los entregue separados, y acepta varios campos")
    void ordenesCompuestos() {
        Pageable p = Paginacion.de(0, 10, List.of("email", "desc", "creadoEn,asc"), PERMITIDOS, POR_DEFECTO);

        assertThat(p.getSort().getOrderFor("email").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(p.getSort().getOrderFor("creadoEn").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("CA-1-32 un campo sin dirección es ascendente y los campos no permitidos se ignoran")
    void camposNoPermitidos() {
        assertThat(Paginacion.de(0, 10, List.of("email"), PERMITIDOS, POR_DEFECTO).getSort().getOrderFor("email").getDirection())
                .isEqualTo(Sort.Direction.ASC);
        assertThat(Paginacion.de(0, 10, List.of("passwordHash,desc", " ,"), PERMITIDOS, POR_DEFECTO).getSort()).isEqualTo(POR_DEFECTO);
        assertThat(Paginacion.de(0, 10, List.of("email", "creadoEn,desc"), PERMITIDOS, POR_DEFECTO).getSort().stream().count()).isEqualTo(2);
    }
}
