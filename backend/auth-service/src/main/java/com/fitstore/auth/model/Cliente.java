package com.fitstore.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Comprador del storefront. */
@Entity
@Table(name = "cliente")
@Getter
@Setter
public class Cliente extends Cuenta {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 8, columnDefinition = "char(8)", unique = true)
    private String dni;

    @Column(nullable = false, length = 15)
    private String telefono;

    @Override
    public TipoCuenta tipo() {
        return TipoCuenta.CLIENTE;
    }

    @Override
    public String rol() {
        return "CLIENTE";
    }
}
