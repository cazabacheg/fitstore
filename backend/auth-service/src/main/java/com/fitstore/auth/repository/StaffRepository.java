package com.fitstore.auth.repository;

import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StaffRepository extends JpaRepository<Staff, UUID>, JpaSpecificationExecutor<Staff> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Staff> findByEmailIgnoreCase(String email);

    Page<Staff> findByRol(RolStaff rol, Pageable pagina);

    /** Invariante 6: siempre debe quedar al menos un ADMIN activo. */
    long countByRolAndActivoTrue(RolStaff rol);

    /** ADMIN que pueden ingresar hoy (activos y con contraseña): los que cuenta la invariante 6. */
    long countByRolAndActivoTrueAndPasswordHashIsNotNull(RolStaff rol);
}
