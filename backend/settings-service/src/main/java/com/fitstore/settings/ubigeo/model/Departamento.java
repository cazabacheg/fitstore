package com.fitstore.settings.ubigeo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Departamento del Perú (código INEI de 2 dígitos). */
@Entity
@Table(name = "departamento")
@Getter
@Setter
@NoArgsConstructor
public class Departamento {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 2, columnDefinition = "char(2)")
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;
}
