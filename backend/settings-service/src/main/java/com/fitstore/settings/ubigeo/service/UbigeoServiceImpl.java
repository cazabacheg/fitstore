package com.fitstore.settings.ubigeo.service;

import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.settings.ubigeo.dto.DistritoEncontradoDto;
import com.fitstore.settings.ubigeo.dto.UbigeoItemDto;
import com.fitstore.settings.ubigeo.dto.UbigeoNombreDto;
import com.fitstore.settings.ubigeo.model.Departamento;
import com.fitstore.settings.ubigeo.model.Distrito;
import com.fitstore.settings.ubigeo.model.Provincia;
import com.fitstore.settings.ubigeo.repository.DepartamentoRepository;
import com.fitstore.settings.ubigeo.repository.DistritoRepository;
import com.fitstore.settings.ubigeo.repository.ProvinciaRepository;
import com.fitstore.settings.ubigeo.repository.UbigeoRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UbigeoServiceImpl implements UbigeoService {

    private static final int MAXIMO_RESULTADOS = 20;

    private final UbigeoRepository ubigeo;
    private final DepartamentoRepository departamentos;
    private final ProvinciaRepository provincias;
    private final DistritoRepository distritos;

    public UbigeoServiceImpl(UbigeoRepository ubigeo, DepartamentoRepository departamentos,
            ProvinciaRepository provincias, DistritoRepository distritos) {
        this.ubigeo = ubigeo;
        this.departamentos = departamentos;
        this.provincias = provincias;
        this.distritos = distritos;
    }

    @Override
    public List<UbigeoItemDto> departamentos() {
        return ubigeo.departamentos().stream()
                .map(d -> new UbigeoItemDto(d.getCodigo(), NombresUbigeo.formatear(d.getNombre()))).toList();
    }

    @Override
    public List<UbigeoItemDto> provincias(String departamentoCodigo) {
        if (!departamentos.existsById(departamentoCodigo)) {
            throw new RecursoNoEncontradoException("No encontramos el departamento " + departamentoCodigo);
        }
        return ubigeo.provinciasDe(departamentoCodigo).stream()
                .map(p -> new UbigeoItemDto(p.getCodigo(), NombresUbigeo.formatear(p.getNombre()))).toList();
    }

    @Override
    public List<UbigeoItemDto> distritos(String provinciaCodigo) {
        if (!provincias.existsById(provinciaCodigo)) {
            throw new RecursoNoEncontradoException("No encontramos la provincia " + provinciaCodigo);
        }
        return ubigeo.distritosDe(provinciaCodigo).stream()
                .map(d -> new UbigeoItemDto(d.getCodigo(), NombresUbigeo.formatear(d.getNombre()))).toList();
    }

    @Override
    public List<DistritoEncontradoDto> buscarDistritos(String texto) {
        return ubigeo.buscarDistritos(texto.trim()).stream().limit(MAXIMO_RESULTADOS)
                .map(d -> new DistritoEncontradoDto(d.getCodigo(), NombresUbigeo.formatear(d.getNombre()),
                        NombresUbigeo.formatear(d.getProvincia().getNombre()),
                        NombresUbigeo.formatear(d.getProvincia().getDepartamento().getNombre())))
                .toList();
    }

    @Override
    public UbigeoNombreDto nombre(String codigo) {
        return switch (codigo.length()) {
            case 2 -> departamento(codigo);
            case 4 -> provincia(codigo);
            default -> distrito(codigo);
        };
    }

    @Override
    public Optional<String> nombreCompletoDeDistrito(String codigo) {
        return ubigeo.nombreCompleto(codigo).map(completo -> String.join(", ",
                Arrays.stream(completo.split(", ")).map(NombresUbigeo::formatear).toList()));
    }

    private UbigeoNombreDto departamento(String codigo) {
        Departamento d = departamentos.findById(codigo).orElseThrow(() -> noExiste(codigo));
        String nombre = NombresUbigeo.formatear(d.getNombre());
        return new UbigeoNombreDto(codigo, nombre, nombre, null, null, nombre);
    }

    private UbigeoNombreDto provincia(String codigo) {
        Provincia p = provincias.findById(codigo).orElseThrow(() -> noExiste(codigo));
        String nombre = NombresUbigeo.formatear(p.getNombre());
        String departamento = NombresUbigeo.formatear(p.getDepartamento().getNombre());
        return new UbigeoNombreDto(codigo, nombre, departamento, nombre, null, nombre + ", " + departamento);
    }

    private UbigeoNombreDto distrito(String codigo) {
        Distrito d = distritos.findById(codigo).orElseThrow(() -> noExiste(codigo));
        String nombre = NombresUbigeo.formatear(d.getNombre());
        String provincia = NombresUbigeo.formatear(d.getProvincia().getNombre());
        String departamento = NombresUbigeo.formatear(d.getProvincia().getDepartamento().getNombre());
        return new UbigeoNombreDto(codigo, nombre, departamento, provincia, nombre,
                nombre + ", " + provincia + ", " + departamento);
    }

    private static RecursoNoEncontradoException noExiste(String codigo) {
        return new RecursoNoEncontradoException("No encontramos el código " + codigo);
    }
}
