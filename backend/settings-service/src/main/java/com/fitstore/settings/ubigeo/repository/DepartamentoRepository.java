package com.fitstore.settings.ubigeo.repository;

import com.fitstore.settings.ubigeo.model.Departamento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartamentoRepository extends JpaRepository<Departamento, String> {

    List<Departamento> findAllByOrderByNombreAsc();
}
