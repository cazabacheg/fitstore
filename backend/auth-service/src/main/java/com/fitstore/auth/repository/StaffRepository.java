package com.fitstore.auth.repository;

import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Staff> findByEmailIgnoreCase(String email);

    Page<Staff> findByRol(RolStaff rol, Pageable pagina);

    /** Invariante 6: siempre debe quedar al menos un ADMIN activo. */
    long countByRolAndActivoTrue(RolStaff rol);
}
