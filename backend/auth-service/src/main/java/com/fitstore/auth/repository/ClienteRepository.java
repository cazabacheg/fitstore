package com.fitstore.auth.repository;

import com.fitstore.auth.model.Cliente;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Cliente> findByEmailIgnoreCase(String email);

    boolean existsByDni(String dni);

    /** Fila del cliente bloqueada hasta el fin de la transacción (serializa el alta de direcciones). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cliente c where c.id = :id")
    Optional<Cliente> bloquearPorId(@Param("id") UUID id);

    /** Búsqueda del backoffice por nombre, apellido, correo o DNI (contiene, sin importar mayúsculas). */
    @Query("""
            select c from Cliente c
            where lower(concat(c.nombres, ' ', c.apellidos)) like lower(concat('%', :texto, '%'))
               or lower(c.email) like lower(concat('%', :texto, '%'))
               or c.dni like concat('%', :texto, '%')
            """)
    Page<Cliente> buscar(@Param("texto") String texto, Pageable pagina);
}
