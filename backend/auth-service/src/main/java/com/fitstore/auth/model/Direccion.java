package com.fitstore.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Libreta de direcciones del cliente. Se elimina físicamente (los pedidos guardan una copia). */
@Entity
@Table(name = "direccion")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 40)
    private String alias;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 6, columnDefinition = "char(6)")
    private String ubigeo;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(length = 200)
    private String referencia;

    /** Si es NULL se usa el teléfono del cliente. */
    @Column(name = "telefono_contacto", length = 15)
    private String telefonoContacto;

    @Column(nullable = false)
    private boolean predeterminada;

    @CreatedDate
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @LastModifiedDate
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;
}
