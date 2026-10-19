package com.fitstore.settings.ubigeo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Provincia (código INEI de 4 dígitos: departamento + provincia). */
@Entity
@Table(name = "provincia")
@Getter
@Setter
@NoArgsConstructor
public class Provincia {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 4, columnDefinition = "char(4)")
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "departamento_codigo", nullable = false)
    private Departamento departamento;

    @Column(nullable = false, length = 100)
    private String nombre;
}
