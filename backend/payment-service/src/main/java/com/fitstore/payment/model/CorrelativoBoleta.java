package com.fitstore.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Último correlativo emitido de una serie de boletas (la serie B001 viene sembrada en V1). */
@Entity
@Table(name = "correlativo_boleta")
@Getter
@Setter
public class CorrelativoBoleta {

    @Id
    @Column(length = 10)
    private String serie;

    @Column(nullable = false)
    private int ultimo;

    protected CorrelativoBoleta() {
    }

    public CorrelativoBoleta(String serie, int ultimo) {
        this.serie = serie;
        this.ultimo = ultimo;
    }
}
