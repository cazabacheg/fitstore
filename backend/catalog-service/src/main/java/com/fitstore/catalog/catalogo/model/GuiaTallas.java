package com.fitstore.catalog.catalogo.model;

import jakarta.persistence.*;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;

/** Guía de tallas de una categoría, como tabla estructurada. */
@Entity
@Table(name = "guia_tallas")
@Getter
@Setter
public class GuiaTallas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    /** Columnas de medidas y una fila por talla (micro 2.3). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> tabla;

    @Column(columnDefinition = "text")
    private String nota;
}
