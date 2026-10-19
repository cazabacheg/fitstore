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

/** Distrito (código INEI de 6 dígitos): el que los demás servicios guardan. */
@Entity
@Table(name = "distrito")
@Getter
@Setter
@NoArgsConstructor
public class Distrito {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 6, columnDefinition = "char(6)")
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provincia_codigo", nullable = false)
    private Provincia provincia;

    @Column(nullable = false, length = 100)
    private String nombre;
}
