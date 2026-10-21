package com.fitstore.notification.service;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.notification.dto.EnvioDto;
import com.fitstore.notification.dto.EnvioResumenDto;
import com.fitstore.notification.dto.IntentoDto;
import com.fitstore.notification.dto.PaginaEnviosDto;
import com.fitstore.notification.model.EnvioCorreo;
import com.fitstore.notification.model.EstadoEnvio;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.repository.EnvioCorreoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvioServiceImpl implements EnvioService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final int TAMANO_MAXIMO = 100;

    private final EnvioCorreoRepository envios;
    private final Clock reloj;

    public EnvioServiceImpl(EnvioCorreoRepository envios, Clock reloj) {
        this.envios = envios;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaEnviosDto listar(EstadoEnvio estado, CatalogoPlantillas plantilla, String destinatario,
            String referencia, LocalDate desde, LocalDate hasta, int pagina, int tamano) {
        String codigo = plantilla == null ? null : plantilla.name();
        String correo = vacioANulo(destinatario);
        String ref = vacioANulo(referencia);
        Instant inicio = desde == null ? null : desde.atStartOfDay(LIMA).toInstant();
        Instant fin = hasta == null ? null : hasta.plusDays(1).atStartOfDay(LIMA).toInstant().minusNanos(1);
        int tam = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);

        Page<EnvioCorreo> pag = envios.filtrar(estado, codigo, correo, ref, inicio, fin, PageRequest.of(Math.max(pagina, 0), tam));
        return new PaginaEnviosDto(pag.getContent().stream().map(EnvioServiceImpl::resumen).toList(), pag.getNumber(),
                pag.getSize(), pag.getTotalElements(), pag.getTotalPages(),
                contadores(envios.contarPorEstado(codigo, correo, ref, inicio, fin)));
    }

    @Override
    @Transactional(readOnly = true)
    public EnvioDto obtener(UUID id) {
        return detalle(buscar(id));
    }

    @Override
    @Transactional
    public EnvioDto reenviar(UUID id) {
        EnvioCorreo envio = buscar(id);
        if (envio.getEstado() != EstadoEnvio.FALLIDO && envio.getEstado() != EstadoEnvio.ENVIADO) {
            throw new ConflictoNegocioException("ENVIO_NO_REENVIABLE",
                    "Solo se pueden reenviar los correos enviados o fallidos");
        }
        envio.setEstado(EstadoEnvio.PENDIENTE);
        envio.setIntentos(0);
        envio.setProximoIntentoEn(reloj.instant());
        return detalle(envios.save(envio));
    }

    private EnvioCorreo buscar(UUID id) {
        return envios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe ese envío"));
    }

    private static Map<String, Long> contadores(List<Object[]> filas) {
        Map<EstadoEnvio, Long> porEstado = new EnumMap<>(EstadoEnvio.class);
        for (EstadoEnvio estado : EstadoEnvio.values()) {
            porEstado.put(estado, 0L);
        }
        filas.forEach(fila -> porEstado.put((EstadoEnvio) fila[0], (Long) fila[1]));
        Map<String, Long> resultado = new LinkedHashMap<>();
        porEstado.forEach((estado, cantidad) -> resultado.put(estado.name(), cantidad));
        return resultado;
    }

    private static EnvioResumenDto resumen(EnvioCorreo e) {
        return new EnvioResumenDto(e.getId(), CatalogoPlantillas.valueOf(e.getPlantilla()), e.getDestinatario(),
                e.getAsunto(), e.getReferencia(), e.getEstado(), e.getIntentos(), e.getAdjunto() != null,
                e.getCreadoEn(), e.getEnviadoEn());
    }

    private static EnvioDto detalle(EnvioCorreo e) {
        List<IntentoDto> intentos = e.getHistorialIntentos().stream()
                .map(i -> new IntentoDto(((Number) i.get("numero")).intValue(), Instant.parse(String.valueOf(i.get("fecha"))),
                        (String) i.get("error")))
                .toList();
        return new EnvioDto(e.getId(), CatalogoPlantillas.valueOf(e.getPlantilla()), e.getDestinatario(), e.getAsunto(),
                e.getReferencia(), e.getEstado(), e.getIntentos(), e.getAdjunto() != null, e.getCreadoEn(),
                e.getEnviadoEn(), intentos, e.getUltimoError(), e.getProximoIntentoEn());
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
