package com.fitstore.settings.reclamaciones.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Último correlativo entregado en un año. Solo se modifica con {@code CorrelativoReclamoRepository#siguiente}. */
@Entity
@Table(name = "correlativo_reclamo")
@Getter
@NoArgsConstructor
public class CorrelativoReclamo {

    @Id
    private Integer anio;

    private int ultimo;
}
