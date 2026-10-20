package com.fitstore.notification.repository;

import com.fitstore.notification.model.MarcaTienda;
import org.springframework.data.jpa.repository.JpaRepository;

/** Fila única ({@code id = 1}): se lee y se actualiza con {@code findById((short) 1)} y {@code save}. */
public interface MarcaTiendaRepository extends JpaRepository<MarcaTienda, Short> {
}
