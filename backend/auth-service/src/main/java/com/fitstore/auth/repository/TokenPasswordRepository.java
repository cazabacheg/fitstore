package com.fitstore.auth.repository;

import com.fitstore.auth.model.TokenPassword;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenPasswordRepository extends JpaRepository<TokenPassword, Long> {

    Optional<TokenPassword> findByTokenHash(String tokenHash);

    /** Tokens sin usar y sin vencer de un cliente (al generar uno nuevo se invalidan estos, invariante 7). */
    List<TokenPassword> findByClienteIdAndUsadoEnIsNullAndExpiraEnAfter(UUID clienteId, Instant ahora);

    List<TokenPassword> findByStaffIdAndUsadoEnIsNullAndExpiraEnAfter(UUID staffId, Instant ahora);
}
