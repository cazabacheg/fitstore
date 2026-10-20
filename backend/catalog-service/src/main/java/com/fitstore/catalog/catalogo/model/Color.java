package com.fitstore.catalog.catalogo.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;

/** Color maestro con su código hexadecimal para la muestra. */
@Entity
@Table(name = "color")
@Getter
@Setter
public class Color {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String nombre;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 7, columnDefinition = "char(7)")
    private String hex;

    @Column(nullable = false)
    private boolean activo = true;
}
