package com.fitstore.settings.configuracion.service;

import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.settings.configuracion.dto.PoliticaDto;
import com.fitstore.settings.configuracion.dto.VersionPoliticaDto;
import com.fitstore.settings.configuracion.model.Politica;
import com.fitstore.settings.configuracion.model.TipoPolitica;
import com.fitstore.settings.configuracion.repository.PoliticaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PoliticaServiceImpl implements PoliticaService {

    private final PoliticaRepository repositorio;
    private final PublicadorConfiguracion publicador;
    private final Clock reloj;

    public PoliticaServiceImpl(PoliticaRepository repositorio, PublicadorConfiguracion publicador, Clock reloj) {
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public PoliticaDto obtener(String tipo, Integer version) {
        TipoPolitica politica = tipoDe(tipo);
        Politica encontrada = (version == null
                ? repositorio.findFirstByTipoOrderByVersionDesc(politica)
                : repositorio.findByTipoAndVersion(politica, version))
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos esa política"));
        return dto(encontrada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VersionPoliticaDto> versiones(String tipo) {
        List<Politica> historial = repositorio.findByTipoOrderByVersionDesc(tipoDe(tipo));
        int vigente = historial.isEmpty() ? 0 : historial.get(0).getVersion();
        return historial.stream().map(p -> new VersionPoliticaDto(p.getVersion(), p.getPublicadaEn(),
                p.getPublicadaPor(), p.getVersion() == vigente)).toList();
    }

    @Override
    @Transactional
    public PoliticaDto publicar(String tipo, String contenido, String autor) {
        TipoPolitica politica = tipoDe(tipo);
        int siguiente = repositorio.findFirstByTipoOrderByVersionDesc(politica).map(p -> p.getVersion() + 1).orElse(1);
        Politica nueva = new Politica();
        nueva.setTipo(politica);
        nueva.setVersion(siguiente);
        nueva.setContenido(contenido);
        nueva.setPublicadaEn(Instant.now(reloj));
        nueva.setPublicadaPor(autor);
        Politica guardada = repositorio.saveAndFlush(nueva);
        publicador.invalidarCache();
        return dto(guardada);
    }

    private static PoliticaDto dto(Politica p) {
        return new PoliticaDto(ruta(p.getTipo()), p.getVersion(), p.getContenido(), p.getPublicadaEn());
    }

    private static String ruta(TipoPolitica tipo) {
        return tipo.name().toLowerCase().replace('_', '-');
    }

    private static TipoPolitica tipoDe(String ruta) {
        for (TipoPolitica tipo : TipoPolitica.values()) {
            if (ruta(tipo).equals(ruta)) {
                return tipo;
            }
        }
        throw new RecursoNoEncontradoException("No encontramos esa política");
    }
}
