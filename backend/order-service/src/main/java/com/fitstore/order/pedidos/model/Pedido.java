package com.fitstore.order.pedidos.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pedido: copia congelada (snapshot) de lo que se compró (D3). Los pedidos nunca se eliminan (invariante 7); su
 * estado cambia con {@code PedidoRepository.cambiarEstadoSi} y cada cambio deja una fila de historial.
 */
@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    private UUID id = UUID.randomUUID();

    /** Código legible {@code FS-000123} (D4). */
    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Modalidad modalidad;

    @Embedded
    private Destinatario destinatario;

    /** Nula en la modalidad recojo. */
    @Embedded
    private DireccionEnvio direccion;

    @Embedded
    private Envio envio;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "descuento_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal descuentoTotal;

    @Column(name = "costo_envio", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoEnvio;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "boleta_url", length = 500)
    private String boletaUrl;

    @Column(name = "pago_proveedor", length = 20)
    private String pagoProveedor;

    @Column(name = "pago_medio", length = 20)
    private String pagoMedio;

    @Column(name = "pagado_en")
    private Instant pagadoEn;

    @Column(name = "monto_reembolsado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoReembolsado = BigDecimal.ZERO;

    /** Cabecera {@code Idempotency-Key} del checkout; única por cliente (D5). */
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(length = 500)
    private String notas;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    /** Nulo hasta el primer guardado; así Spring Data sabe que el pedido (con id asignado) es nuevo. */
    @Version
    private Long version;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<PedidoItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<HistorialEstado> historial = new ArrayList<>();

    public void agregarItem(PedidoItem item) {
        item.setPedido(this);
        items.add(item);
    }

    public void agregarHistorial(EstadoPedido anterior, EstadoPedido nuevo, TipoActor actor, UUID actorId,
            String comentario) {
        HistorialEstado fila = new HistorialEstado();
        fila.setPedido(this);
        fila.setEstadoAnterior(anterior);
        fila.setEstadoNuevo(nuevo);
        fila.setActorTipo(actor);
        fila.setActorId(actorId);
        fila.setComentario(comentario);
        historial.add(fila);
    }

    @PrePersist
    void antesDeGuardar() {
        Instant ahora = Instant.now();
        if (creadoEn == null) {
            creadoEn = ahora;
        }
        if (actualizadoEn == null) {
            actualizadoEn = ahora;
        }
    }

    @PreUpdate
    void antesDeActualizar() {
        actualizadoEn = Instant.now();
    }
}
