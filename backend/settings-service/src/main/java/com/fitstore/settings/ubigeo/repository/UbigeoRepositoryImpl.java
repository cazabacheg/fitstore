package com.fitstore.settings.ubigeo.repository;

import com.fitstore.settings.ubigeo.model.Departamento;
import com.fitstore.settings.ubigeo.model.Distrito;
import com.fitstore.settings.ubigeo.model.Provincia;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class UbigeoRepositoryImpl implements UbigeoRepository {

    private final DepartamentoRepository departamentos;
    private final ProvinciaRepository provincias;
    private final DistritoRepository distritos;

    public UbigeoRepositoryImpl(DepartamentoRepository departamentos, ProvinciaRepository provincias,
            DistritoRepository distritos) {
        this.departamentos = departamentos;
        this.provincias = provincias;
        this.distritos = distritos;
    }

    @Override
    public List<Departamento> departamentos() {
        return departamentos.findAllByOrderByNombreAsc();
    }

    @Override
    public List<Provincia> provinciasDe(String departamentoCodigo) {
        return provincias.findByDepartamentoCodigoOrderByNombreAsc(departamentoCodigo);
    }

    @Override
    public List<Distrito> distritosDe(String provinciaCodigo) {
        return distritos.findByProvinciaCodigoOrderByNombreAsc(provinciaCodigo);
    }

    @Override
    public List<Distrito> buscarDistritos(String texto) {
        return distritos.buscarPorNombre(texto);
    }

    @Override
    public Optional<String> nombreCompleto(String distritoCodigo) {
        return distritos.findById(distritoCodigo).map(d -> "%s, %s, %s".formatted(d.getNombre(),
                d.getProvincia().getNombre(), d.getProvincia().getDepartamento().getNombre()));
    }
}
