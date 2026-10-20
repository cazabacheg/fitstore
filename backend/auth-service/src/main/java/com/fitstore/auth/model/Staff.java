package com.fitstore.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Personal del backoffice (OPERADOR o ADMIN). */
@Entity
@Table(name = "staff")
@Getter
@Setter
public class Staff extends Cuenta {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolStaff rol;

    /** Quién lo dio de alta; NULL para el admin inicial. */
    @Column(name = "creado_por")
    private UUID creadoPor;

    @Override
    public TipoCuenta tipo() {
        return TipoCuenta.STAFF;
    }

    @Override
    public String rol() {
        return rol.name();
    }
}
