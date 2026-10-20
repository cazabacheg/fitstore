package com.fitstore.delivery.repository;

import com.fitstore.delivery.model.Repartidor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepartidorRepository extends JpaRepository<Repartidor, Long> {

    List<Repartidor> findByActivoTrue();
}
