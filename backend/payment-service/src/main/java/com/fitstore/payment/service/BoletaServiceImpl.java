package com.fitstore.payment.service;

import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.common.event.OutboxRegistrador;
import com.fitstore.common.storage.FileStorage;
import com.fitstore.common.storage.TipoBucket;
import com.fitstore.payment.client.ConfiguracionTienda;
import com.fitstore.payment.config.PropiedadesPagos;
import com.fitstore.payment.dto.ConfiguracionPublica;
import com.fitstore.payment.event.BoletaEmitida;
import com.fitstore.payment.model.Boleta;
import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.repository.BoletaRepository;
import com.fitstore.payment.repository.CorrelativoBoletaRepository;
import com.fitstore.payment.repository.IntencionPagoRepository;
import com.fitstore.payment.repository.TransaccionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class BoletaServiceImpl implements BoletaService {

    private static final Logger LOG = LoggerFactory.getLogger(BoletaServiceImpl.class);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final BigDecimal IGV = new BigDecimal("1.18");
    private static final String PREFIJO_MEDIA = "/media/";

    private final IntencionPagoRepository intenciones;
    private final TransaccionRepository transacciones;
    private final BoletaRepository boletas;
    private final CorrelativoBoletaRepository correlativos;
    private final ConfiguracionTienda tienda;
    private final FileStorage almacenamiento;
    private final OutboxRegistrador outbox;
    private final BoletaPdf pdf;
    private final PropiedadesPagos propiedades;
    private final TransactionTemplate transaccion;

    public BoletaServiceImpl(IntencionPagoRepository intenciones, TransaccionRepository transacciones,
            BoletaRepository boletas, CorrelativoBoletaRepository correlativos, ConfiguracionTienda tienda,
            FileStorage almacenamiento, OutboxRegistrador outbox, BoletaPdf pdf, PropiedadesPagos propiedades,
            TransactionTemplate transaccion) {
        this.intenciones = intenciones;
        this.transacciones = transacciones;
        this.boletas = boletas;
        this.correlativos = correlativos;
        this.tienda = tienda;
        this.almacenamiento = almacenamiento;
        this.outbox = outbox;
        this.pdf = pdf;
        this.propiedades = propiedades;
        this.transaccion = transaccion;
    }

    @Override
    public Boleta emitir(UUID intencionId) {
        return transaccion.execute(estado -> generar(intencionId));
    }

    @Override
    public int emitirPendientes() {
        int emitidas = 0;
        for (IntencionPago pendiente : intenciones.sinBoleta()) {
            try {
                emitir(pendiente.getId());
                emitidas++;
            } catch (RuntimeException e) {
                // El pago sigue confirmado; la boleta se reintenta en la siguiente pasada
                LOG.warn("No se pudo emitir la boleta del pedido {}: {}", pendiente.getCodigoPedido(), e.getMessage());
            }
        }
        return emitidas;
    }

    @Override
    public DescargaBoleta descargarDeLaClienta(String codigoPedido, UUID clienteId) {
        IntencionPago intencion = intenciones.findByCodigoPedido(codigoPedido)
                .filter(i -> i.getClienteId().equals(clienteId))
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos esa boleta"));
        return descargarDeLaIntencion(intencion.getId());
    }

    @Override
    public DescargaBoleta descargarDeLaIntencion(UUID intencionId) {
        return boletas.findByIntencionId(intencionId).map(this::leer)
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos esa boleta"));
    }

    @Override
    public DescargaBoleta descargarPorNumero(String numero) {
        return boletas.findByNumero(numero).map(this::leer)
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos esa boleta"));
    }

    private DescargaBoleta leer(Boleta boleta) {
        return new DescargaBoleta(boleta.getNumero(), almacenamiento.leer(TipoBucket.PRIVADO, boleta.getClave()));
    }

    /** Se ejecuta dentro de la transacción de {@link #emitir}: si algo falla, el correlativo no se consume. */
    private Boleta generar(UUID intencionId) {
        IntencionPago intencion = intenciones.bloquearPorId(intencionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos ese pago"));
        Optional<Boleta> existente = boletas.findByIntencionId(intencionId);
        if (existente.isPresent()) {
            return existente.get();
        }
        ConfiguracionPublica.Identidad emisor = tienda.publica().map(ConfiguracionPublica::identidad)
                .orElseThrow(() -> new IllegalStateException("No hay datos del emisor: settings no responde"));
        Transaccion cobro = transacciones.findByIntencionIdOrderByCreadoEnAsc(intencionId).stream()
                .filter(t -> t.getEstado() == EstadoTransaccion.APROBADA).findFirst().orElse(null);

        String serie = propiedades.boleta().serie();
        int correlativo = correlativos.siguiente(serie);
        String numero = "%s-%08d".formatted(serie, correlativo);
        Instant ahora = Instant.now();
        byte[] contenido = pdf.generar(variables(intencion, emisor, cobro, numero, ahora));

        String clave = "boletas/%d/%s.pdf".formatted(ZonedDateTime.ofInstant(ahora, LIMA).getYear(), numero);
        almacenamiento.guardar(TipoBucket.PRIVADO, clave, contenido, "application/pdf");

        Boleta boleta = new Boleta();
        boleta.setIntencionId(intencionId);
        boleta.setSerie(serie);
        boleta.setCorrelativo(correlativo);
        boleta.setNumero(numero);
        boleta.setClave(clave);
        boleta.setEmitidaEn(ahora);
        boleta = boletas.saveAndFlush(boleta);
        outbox.registrar("BoletaEmitida", intencion.getPedidoId().toString(),
                new BoletaEmitida(intencion.getPedidoId(), numero, "/api/v1/pagos/" + intencion.getCodigoPedido() + "/boleta"));
        return boleta;
    }

    private Map<String, Object> variables(IntencionPago intencion, ConfiguracionPublica.Identidad emisor,
            Transaccion cobro, String numero, Instant ahora) {
        BigDecimal total = intencion.getMonto().setScale(2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(total.divide(IGV, 2, RoundingMode.HALF_UP));
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("numero", numero);
        v.put("fecha", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.of("es", "PE"))
                .format(ZonedDateTime.ofInstant(ahora, LIMA)));
        v.put("emisor", emisor);
        v.put("logo", logo(emisor.logoClaroUrl()));
        v.put("cliente", intencion.getCliente());
        v.put("codigoPedido", intencion.getCodigoPedido());
        v.put("medioPago", medioPago(cobro));
        v.put("lineas", intencion.getItems().stream().map(BoletaServiceImpl::linea).toList());
        v.put("subtotal", plano(intencion.getSubtotal()));
        v.put("descuentos", plano(intencion.getDescuentoTotal()));
        v.put("envio", plano(intencion.getCostoEnvio()));
        v.put("total", plano(total));
        v.put("igv", plano(igv));
        v.put("leyenda", propiedades.boleta().leyenda());
        return v;
    }

    /** El logo viene de la configuración pública como {@code /media/<clave>}: se lee del bucket público y se incrusta. */
    private String logo(String url) {
        if (url == null || !url.startsWith(PREFIJO_MEDIA)) {
            return null;
        }
        String clave = url.substring(PREFIJO_MEDIA.length());
        String tipo = clave.toLowerCase(Locale.ROOT).endsWith(".png") ? "image/png"
                : clave.toLowerCase(Locale.ROOT).matches(".*\\.jpe?g") ? "image/jpeg" : null;
        if (tipo == null) {
            return null;
        }
        try {
            return "data:" + tipo + ";base64," + Base64.getEncoder().encodeToString(almacenamiento.leer(TipoBucket.PUBLICO, clave));
        } catch (RuntimeException e) {
            LOG.warn("No se pudo leer el logo {} para la boleta; se emite sin logo", url);
            return null;
        }
    }

    private static String medioPago(Transaccion cobro) {
        if (cobro == null) {
            return "-";
        }
        String proveedor = cobro.getProveedor() == Proveedor.MERCADO_PAGO ? "Mercado Pago"
                : cobro.getProveedor() == Proveedor.SIMULADO ? "Pago de prueba" : "Culqi";
        Object descripcion = cobro.getRespuesta() == null ? null : cobro.getRespuesta().get("descripcionMedio");
        return descripcion == null ? proveedor : proveedor + " - " + descripcion;
    }

    private static LineaBoleta linea(Map<String, Object> item) {
        String talla = texto(item.get("talla"));
        String color = texto(item.get("color"));
        String detalle = (talla == null ? "" : "Talla " + talla) + (talla != null && color != null ? " / " : "")
                + (color == null ? "" : color);
        return new LineaBoleta(texto(item.get("producto")), detalle,
                item.get("cantidad") instanceof Number n ? n.intValue() : 0, plano(monto(item.get("precioUnitario"))),
                plano(monto(item.get("descuentoMonto"))), plano(monto(item.get("total"))));
    }

    private static String texto(Object valor) {
        return valor == null || valor.toString().isBlank() ? null : valor.toString();
    }

    private static BigDecimal monto(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }

    private static String plano(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Una fila de la tabla de la boleta, ya formateada. */
    public record LineaBoleta(String producto, String detalle, int cantidad, String precio, String descuento, String total) {
    }
}
