package com.fitstore.settings.ubigeo.repository;

import com.fitstore.settings.ubigeo.model.Provincia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvinciaRepository extends JpaRepository<Provincia, String> {

    List<Provincia> findByDepartamentoCodigoOrderByNombreAsc(String departamentoCodigo);
}
