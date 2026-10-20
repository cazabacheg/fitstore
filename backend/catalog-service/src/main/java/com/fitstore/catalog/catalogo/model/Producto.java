package com.fitstore.catalog.catalogo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/** Producto del catálogo; el stock vive en el módulo inventario. */
@Entity
@Table(name = "producto")
@Getter
@Setter
public class Producto extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Base del SKU de sus variantes. */
    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 180)
    private String slug;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(name = "precio_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBase;

    /** El menor precio de las variantes activas; sirve para filtrar y ordenar por precio. */
    @Column(name = "precio_desde", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioDesde;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoProducto estado = EstadoProducto.BORRADOR;

    @Column(nullable = false)
    private boolean destacado;

    @Column(name = "orden_destacado")
    private Integer ordenDestacado;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_descuento", nullable = false, length = 20)
    private ModoDescuento modoDescuento = ModoDescuento.GLOBAL;

    @ManyToMany
    @JoinTable(name = "producto_categoria",
            joinColumns = @JoinColumn(name = "producto_id"),
            inverseJoinColumns = @JoinColumn(name = "categoria_id"))
    private Set<Categoria> categorias = new HashSet<>();

    @Version
    private Integer version;
}
