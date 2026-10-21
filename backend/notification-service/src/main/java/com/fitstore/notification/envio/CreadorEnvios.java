package com.fitstore.notification.envio;

import com.fitstore.notification.model.EnvioCorreo;
import com.fitstore.notification.model.EstadoEnvio;
import com.fitstore.notification.model.Plantilla;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.MarcaService;
import com.fitstore.notification.plantillas.SustitucionVariables;
import com.fitstore.notification.repository.EnvioCorreoRepository;
import com.fitstore.notification.repository.PlantillaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Crea los envíos PENDIENTE que piden los manejadores de eventos (micro 4.2). Respeta la plantilla desactivada por el
 * admin (CA-7-04) y guarda el asunto ya armado para que el registro lo muestre. Se usa dentro de la transacción del
 * consumidor, que además marca el evento como procesado.
 */
@Component
public class CreadorEnvios {

    private static final Logger LOG = LoggerFactory.getLogger(CreadorEnvios.class);

    private final EnvioCorreoRepository envios;
    private final PlantillaRepository plantillas;
    private final MarcaService marca;
    private final Clock reloj;

    public CreadorEnvios(EnvioCorreoRepository envios, PlantillaRepository plantillas, MarcaService marca, Clock reloj) {
        this.envios = envios;
        this.plantillas = plantillas;
        this.marca = marca;
        this.reloj = reloj;
    }

    /**
     * @param eventoId     evento que lo origina; nulo en el respaldo de la boleta (lo origina un temporizador)
     * @param destinatario correo del cliente o de la tienda; si falta no se crea nada
     * @param referencia   código de pedido o de reclamo, para el registro
     * @param adjunto      número de la boleta a adjuntar, o nulo
     * @return el envío creado, o vacío si la plantilla está desactivada o no hay destinatario
     */
    public Optional<EnvioCorreo> crear(UUID eventoId, CatalogoPlantillas catalogo, String destinatario,
            Map<String, Object> variables, String referencia, String adjunto) {
        if (destinatario == null || destinatario.isBlank()) {
            LOG.warn("Evento {}: no hay destinatario para {}; no se crea el envío", eventoId, catalogo);
            return Optional.empty();
        }
        Plantilla plantilla = plantillas.findById(catalogo.name())
                .orElseThrow(() -> new IllegalStateException("La plantilla " + catalogo + " no está sembrada"));
        if (!plantilla.isActiva()) {
            return Optional.empty();
        }
        Map<String, Object> vars = new HashMap<>(variables);
        vars.put("nombreTienda", marca.actual().nombre());
        Instant ahora = reloj.instant();

        EnvioCorreo envio = new EnvioCorreo();
        envio.setId(UUID.randomUUID());
        envio.setEventoId(eventoId);
        envio.setPlantilla(catalogo.name());
        envio.setDestinatario(destinatario.trim());
        envio.setAsunto(abreviar(SustitucionVariables.sustituir(plantilla.getAsunto(), vars), 255));
        envio.setVariables(variables);
        envio.setAdjunto(adjunto);
        envio.setEstado(EstadoEnvio.PENDIENTE);
        envio.setIntentos(0);
        envio.setHistorialIntentos(new ArrayList<>());
        envio.setProximoIntentoEn(ahora);
        envio.setReferencia(referencia);
        envio.setCreadoEn(ahora);
        return Optional.of(envios.save(envio));
    }

    /** Un envío por cada "correo para alertas" de la tienda (T1 a T4, D5). */
    public List<EnvioCorreo> crearAlertas(UUID eventoId, CatalogoPlantillas catalogo, Map<String, Object> variables,
            String referencia) {
        List<EnvioCorreo> creados = new ArrayList<>();
        for (String correo : marca.correosAlertas()) {
            crear(eventoId, catalogo, correo, variables, referencia, null).ifPresent(creados::add);
        }
        return creados;
    }

    private static String abreviar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
