package com.fitstore.settings.reclamaciones.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fitstore.settings.reclamaciones.model.EstadoReclamo;
import com.fitstore.settings.reclamaciones.model.TipoBien;
import com.fitstore.settings.reclamaciones.model.TipoDocumento;
import com.fitstore.settings.reclamaciones.model.TipoReclamo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Detalle de un reclamo para el staff: la hoja completa, el plazo y la respuesta. */
public record ReclamoDto(Long id, String numero, TipoReclamo tipo, EstadoReclamo estado, String consumidorNombre,
        Instant creadoEn, LocalDate fechaLimite, boolean porVencer, boolean vencido, ConsumidorDto consumidor,
        BienDto bien, String detalle, String pedidoConsumidor, String respuesta, Instant respondidoEn,
        String respondidoPor) {

    /** Datos del consumidor (privados: solo el staff los ve). */
    public record ConsumidorDto(String nombres, String apellidos, TipoDocumento tipoDocumento,
            String numeroDocumento, String domicilio, String telefono, String email, boolean esMenor,
            @JsonInclude(JsonInclude.Include.ALWAYS) String apoderadoNombre) {
    }

    /** Bien contratado. */
    public record BienDto(TipoBien tipo, String descripcion, BigDecimal montoReclamado, String pedidoCodigo) {
    }
}
