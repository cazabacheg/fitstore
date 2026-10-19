package com.fitstore.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Credenciales comunes de {@link Cliente} y {@link Staff} (D2: sin tabla común). */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    /** BCrypt. En el staff es NULL mientras no establezca su contraseña (invitación pendiente). */
    @Column(name = "password_hash", length = 60)
    private String passwordHash;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 80)
    private String apellidos;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "credenciales_actualizadas_en", nullable = false)
    private Instant credencialesActualizadasEn;

    @Column(name = "ultimo_login_en")
    private Instant ultimoLoginEn;

    @CreatedDate
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @LastModifiedDate
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Version
    private Integer version;

    public abstract TipoCuenta tipo();

    /** Rol del JWT: {@code CLIENTE} o el rol del staff. */
    public abstract String rol();

    @PrePersist
    void alCrear() {
        if (credencialesActualizadasEn == null) {
            credencialesActualizadasEn = Instant.now();
        }
    }
}
