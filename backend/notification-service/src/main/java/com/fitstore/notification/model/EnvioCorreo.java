package com.fitstore.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Un correo por fila: registro de envíos con estado y reintentos (micro 3.1, D6). */
@Entity
@Table(name = "envio_correo")
@Getter
@Setter
@NoArgsConstructor
public class EnvioCorreo {

    @Id
    private UUID id;

    /** Evento que lo originó; nulo en los correos de prueba del editor. */
    @Column(name = "evento_id")
    private UUID eventoId;

    /** Código de la {@link Plantilla}. */
    @Column(nullable = false, length = 40)
    private String plantilla;

    @Column(nullable = false, length = 254)
    private String destinatario;

    @Column(nullable = false, length = 255)
    private String asunto;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> variables;

    /** Número de la boleta adjunta, o nulo. */
    @Column(length = 20)
    private String adjunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEnvio estado;

    @Column(nullable = false)
    private int intentos;

    /** {@code [{numero, fecha, error}]}: lo muestra el detalle del registro. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "historial_intentos", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> historialIntentos;

    @Column(name = "proximo_intento_en")
    private Instant proximoIntentoEn;

    @Column(name = "ultimo_error", columnDefinition = "text")
    private String ultimoError;

    /** Código de pedido o de reclamo. */
    @Column(length = 30)
    private String referencia;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "enviado_en")
    private Instant enviadoEn;
}
