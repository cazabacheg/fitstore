package com.fitstore.order.pedidos.service;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.FitstoreException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.common.event.OutboxRegistrador;
import com.fitstore.order.carrito.model.CarritoId;
import com.fitstore.order.carrito.repository.CarritoRepository;
import com.fitstore.order.client.AuthClient;
import com.fitstore.order.client.CatalogClient;
import com.fitstore.order.client.Dependencias;
import com.fitstore.order.client.DeliveryClient;
import com.fitstore.order.client.SettingsClient;
import com.fitstore.order.client.dto.CotizacionCatalog;
import com.fitstore.order.client.dto.CotizacionCatalog.LineaCotizada;
import com.fitstore.order.client.dto.CotizacionEnvio;
import com.fitstore.order.client.dto.CotizarCatalogRequest;
import com.fitstore.order.client.dto.CotizarEnvioRequest;
import com.fitstore.order.client.dto.DireccionCliente;
import com.fitstore.order.client.dto.PerfilCliente;
import com.fitstore.order.client.dto.ReservaCatalogRequest;
import com.fitstore.order.client.dto.UbigeoNombre;
import com.fitstore.order.config.PropiedadesOrder;
import com.fitstore.order.event.PedidoCreado;
import com.fitstore.order.pedidos.dto.ConfirmarPedidoRequest;
import com.fitstore.order.pedidos.dto.EntregaElegidaRequest;
import com.fitstore.order.pedidos.dto.EnvioDto;
import com.fitstore.order.pedidos.dto.ItemPedidoDto;
import com.fitstore.order.pedidos.dto.PedidoCreadoDto;
import com.fitstore.order.pedidos.dto.ResumenCheckoutDto;
import com.fitstore.order.pedidos.model.Destinatario;
import com.fitstore.order.pedidos.model.DireccionEnvio;
import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Envio;
import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.Pedido;
import com.fitstore.order.pedidos.model.PedidoItem;
import com.fitstore.order.pedidos.model.TipoActor;
import com.fitstore.order.pedidos.repository.PedidoRepository;
import com.fitstore.order.pedidos.service.MontosPedido.Item;
import com.fitstore.order.pedidos.service.MontosPedido.Montos;
import com.fitstore.order.pedidos.service.ParametrosTienda.Parametros;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Resumen y confirmación del checkout (micro 4.3, paso T1 de la saga). El resumen no reserva ni guarda nada; la
 * confirmación es idempotente por {@code Idempotency-Key}, congela precios y datos en el pedido, reserva el stock por
 * contrato con catalog y publica {@code PedidoCreado}.
 */
@Service
public class CheckoutService {

    /** Resultado de confirmar: el pedido creado y si es nuevo (201) o el de una confirmación anterior (200). */
    public record Confirmacion(PedidoCreadoDto pedido, boolean nuevo) {
    }

    /** Todo lo que se junta de los otros servicios para armar el resumen o el pedido. */
    private record Datos(Parametros parametros, PerfilCliente perfil, DireccionEnvio direccion, List<LineaCotizada> lineas,
            Map<Long, Integer> carrito, CotizacionEnvio envio, Modalidad modalidad, Long agenciaId, Montos montos) {
    }

    private final CarritoRepository carritos;
    private final CatalogClient catalog;
    private final AuthClient auth;
    private final DeliveryClient delivery;
    private final SettingsClient settings;
    private final Dependencias dependencias;
    private final ParametrosTienda parametros;
    private final PedidoRepository pedidos;
    private final OutboxRegistrador outbox;
    private final TransactionTemplate transaccion;
    private final PropiedadesOrder propiedades;
    private final JsonMapper json;

    public CheckoutService(CarritoRepository carritos, CatalogClient catalog, AuthClient auth, DeliveryClient delivery,
            SettingsClient settings, Dependencias dependencias, ParametrosTienda parametros, PedidoRepository pedidos,
            OutboxRegistrador outbox, TransactionTemplate transaccion, PropiedadesOrder propiedades, JsonMapper json) {
        this.carritos = carritos;
        this.catalog = catalog;
        this.auth = auth;
        this.delivery = delivery;
        this.settings = settings;
        this.dependencias = dependencias;
        this.parametros = parametros;
        this.pedidos = pedidos;
        this.outbox = outbox;
        this.transaccion = transaccion;
        this.propiedades = propiedades;
        this.json = json;
    }

    /** Ítems, descuentos, costo y plazo del envío y total para la entrega elegida; no reserva ni guarda nada. */
    public ResumenCheckoutDto resumir(UUID clienteId, EntregaElegidaRequest entrega) {
        Datos datos = recopilar(clienteId, entrega);
        List<ItemPedidoDto> items = datos.lineas().stream().map(CheckoutService::aItem).toList();
        return new ResumenCheckoutDto(items, datos.montos().subtotal(), datos.montos().descuentoTotal(),
                datos.montos().costoEnvio(), datos.montos().total(), envioDto(datos),
                PedidoMapper.direccion(datos.direccion()), datos.parametros().expiracionMinutos());
    }

    public Confirmacion confirmar(UUID clienteId, ConfirmarPedidoRequest peticion, String idempotencyKey) {
        Optional<Pedido> previo = pedidos.findByClienteIdAndIdempotencyKey(clienteId, idempotencyKey);
        if (previo.isPresent()) {
            return new Confirmacion(aCreado(previo.get()), false);
        }
        Datos datos = recopilar(clienteId, peticion.entrega());
        if (datos.montos().total().compareTo(peticion.totalEsperado()) != 0) {
            throw new ConflictoNegocioException("PRECIO_CAMBIADO", "Los precios cambiaron, revisa tu pedido");
        }
        Pedido pedido = armar(clienteId, peticion, idempotencyKey, datos);
        reservar(pedido, datos);
        try {
            transaccion.executeWithoutResult(estado -> {
                pedidos.save(pedido);
                outbox.registrar("PedidoCreado", pedido.getId().toString(), eventoCreado(pedido));
            });
        } catch (DataIntegrityViolationException e) {
            // Dos confirmaciones simultáneas con la misma clave: gana la primera, la otra devuelve ese pedido
            Optional<Pedido> ganador = pedidos.findByClienteIdAndIdempotencyKey(clienteId, idempotencyKey);
            if (ganador.isPresent()) {
                return new Confirmacion(aCreado(ganador.get()), false);
            }
            throw e;
        }
        carritos.vaciar(CarritoId.cliente(clienteId));
        return new Confirmacion(aCreado(pedido), true);
    }

    // ---------- recopilación ----------

    private Datos recopilar(UUID clienteId, EntregaElegidaRequest entrega) {
        Map<Long, Integer> carrito = new java.util.TreeMap<>(carritos.obtenerLineas(CarritoId.cliente(clienteId)));
        if (carrito.isEmpty()) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "CARRITO_VACIO", "Tu carrito está vacío");
        }
        Modalidad modalidad = entrega.modalidad();
        Parametros parametrosTienda = parametros.obtener();
        if (!parametrosTienda.modalidades().contains(modalidad)) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "MODALIDAD_NO_DISPONIBLE",
                    "Esa forma de entrega no está disponible en la tienda");
        }
        if (modalidad == Modalidad.AGENCIA && entrega.agenciaId() == null) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "AGENCIA_REQUERIDA", "Elige una agencia");
        }
        PerfilCliente perfil = dependencias.llamar(auth::perfil).data();
        DireccionEnvio direccion = null;
        if (modalidad != Modalidad.RECOJO) {
            direccion = direccionElegida(modalidad, entrega.direccionId());
        }
        List<LineaCotizada> lineas = cotizar(carrito);
        CotizacionEnvio envio = modalidad == Modalidad.RECOJO ? null : cotizarEnvio(modalidad, direccion, entrega.agenciaId());
        BigDecimal costoEnvio = envio == null ? BigDecimal.ZERO : envio.costo();
        List<Item> items = lineas.stream().map(l -> new Item(l.precioUnitario(), l.cantidad(), l.descuentoMonto())).toList();
        return new Datos(parametrosTienda, perfil, direccion, lineas, carrito, envio, modalidad, entrega.agenciaId(),
                MontosPedido.calcular(items, costoEnvio));
    }

    /** La dirección del cliente (auth) con los nombres del ubigeo (settings); delivery local solo en Lima y Callao. */
    private DireccionEnvio direccionElegida(Modalidad modalidad, Long direccionId) {
        if (direccionId == null) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "DIRECCION_REQUERIDA", "Elige una dirección de entrega");
        }
        DireccionCliente direccion;
        try {
            direccion = dependencias.llamar(() -> auth.direccion(direccionId)).data();
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new RecursoNoEncontradoException("No encontramos esa dirección");
            }
            throw Dependencias.noDisponible(e);
        }
        if (modalidad == Modalidad.DELIVERY_LOCAL && !esLimaOCallao(direccion.ubigeo())) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "FUERA_DE_COBERTURA",
                    "El delivery local solo llega a Lima Metropolitana y el Callao");
        }
        UbigeoNombre nombres;
        try {
            nombres = dependencias.llamar(() -> settings.ubigeo(direccion.ubigeo())).data();
        } catch (FeignException e) {
            throw Dependencias.noDisponible(e);
        }
        return new DireccionEnvio(direccion.ubigeo(), nombres.departamento(), nombres.provincia(), nombres.distrito(),
                direccion.direccion(), direccion.referencia());
    }

    /** Invariante 3: los ubigeos de Lima Metropolitana empiezan con 1501 y los del Callao con 07. */
    static boolean esLimaOCallao(String ubigeo) {
        return ubigeo != null && (ubigeo.startsWith("1501") || ubigeo.startsWith("07"));
    }

    /** Cotiza el carrito; una línea no disponible o sin stock detiene el checkout con 409. */
    private List<LineaCotizada> cotizar(Map<Long, Integer> carrito) {
        List<CotizarCatalogRequest.Linea> pedidas = carrito.entrySet().stream()
                .map(e -> new CotizarCatalogRequest.Linea(e.getKey(), e.getValue())).toList();
        var respuesta = dependencias.llamar(() -> catalog.cotizar(new CotizarCatalogRequest(pedidas)));
        if (respuesta == null || respuesta.data() == null) {
            throw Dependencias.noDisponible(null);
        }
        List<LineaCotizada> lineas = respuesta.data().lineas().stream()
                .sorted(Comparator.comparing(LineaCotizada::varianteId)).toList();
        List<LineaCotizada> noDisponibles = lineas.stream()
                .filter(l -> CotizacionCatalog.NO_DISPONIBLE.equals(l.estado())).toList();
        if (!noDisponibles.isEmpty()) {
            throw new ConflictoNegocioException("LINEAS_NO_DISPONIBLES",
                    "Algunos productos ya no están disponibles: " + nombres(noDisponibles),
                    Map.<String, Object>of("varianteIds", noDisponibles.stream().map(LineaCotizada::varianteId).toList()));
        }
        List<LineaCotizada> sinStock = lineas.stream()
                .filter(l -> CotizacionCatalog.STOCK_INSUFICIENTE.equals(l.estado())).toList();
        if (!sinStock.isEmpty()) {
            List<Map<String, Object>> faltantes = sinStock.stream().map(l -> faltante(l.varianteId(), l.cantidad(),
                    l.disponible() == null ? 0 : l.disponible())).toList();
            throw sinStock(nombres(sinStock), faltantes);
        }
        return lineas;
    }

    private CotizacionEnvio cotizarEnvio(Modalidad modalidad, DireccionEnvio direccion, Long agenciaId) {
        CotizarEnvioRequest peticion = new CotizarEnvioRequest(modalidad.name(), direccion.getUbigeo(),
                modalidad == Modalidad.AGENCIA ? agenciaId : null);
        try {
            return dependencias.llamar(() -> delivery.cotizar(peticion)).data();
        } catch (FeignException e) {
            if (e.status() == 422) {
                throw new FitstoreException(HttpStatus.UNPROCESSABLE_CONTENT, "SIN_COBERTURA",
                        "No realizamos envíos por esta modalidad a este distrito");
            }
            if (e.status() == 400) {
                throw new FitstoreException(HttpStatus.BAD_REQUEST, "ENVIO_INVALIDO",
                        "No pudimos cotizar el envío con esos datos");
            }
            throw Dependencias.noDisponible(e);
        }
    }

    // ---------- confirmación ----------

    private Pedido armar(UUID clienteId, ConfirmarPedidoRequest peticion, String clave, Datos datos) {
        Pedido pedido = new Pedido();
        pedido.setCodigo(propiedades.codigoPrefijo() + "-"
                + String.format("%06d", pedidos.siguienteNumeroCodigo()));
        pedido.setClienteId(clienteId);
        pedido.setEstado(EstadoPedido.PENDIENTE_PAGO);
        pedido.setModalidad(datos.modalidad());
        PerfilCliente perfil = datos.perfil();
        pedido.setDestinatario(new Destinatario(perfil.nombres(), perfil.apellidos(), perfil.dni(), perfil.telefono(),
                perfil.email()));
        pedido.setDireccion(datos.direccion());
        CotizacionEnvio envio = datos.envio();
        pedido.setEnvio(envio == null ? new Envio(null, null, null, null, null)
                : new Envio(datos.modalidad() == Modalidad.AGENCIA ? datos.agenciaId() : null, envio.agenciaNombre(),
                        envio.plazoDiasMin(), envio.plazoDiasMax(), null));
        pedido.setSubtotal(datos.montos().subtotal());
        pedido.setDescuentoTotal(datos.montos().descuentoTotal());
        pedido.setCostoEnvio(datos.montos().costoEnvio());
        pedido.setTotal(datos.montos().total());
        pedido.setExpiraEn(Instant.now().plus(datos.parametros().expiracionMinutos(), ChronoUnit.MINUTES));
        pedido.setIdempotencyKey(clave);
        pedido.setNotas(peticion.notas());
        for (LineaCotizada linea : datos.lineas()) {
            PedidoItem item = new PedidoItem();
            item.setVarianteId(linea.varianteId());
            item.setProductoId(linea.productoId());
            item.setSku(linea.sku());
            item.setProductoNombre(linea.producto());
            item.setTalla(linea.talla());
            item.setColor(linea.color());
            item.setImagenUrl(linea.imagenUrl());
            item.setPrecioUnitario(linea.precioUnitario());
            item.setCantidad(linea.cantidad());
            item.setDescuentoPorcentaje(linea.descuentoPorcentaje() == null ? BigDecimal.ZERO : linea.descuentoPorcentaje());
            item.setDescuentoMonto(linea.descuentoMonto());
            item.setTotal(MontosPedido.totalItem(new Item(linea.precioUnitario(), linea.cantidad(), linea.descuentoMonto())));
            pedido.agregarItem(item);
        }
        pedido.agregarHistorial(null, EstadoPedido.PENDIENTE_PAGO, TipoActor.CLIENTE, clienteId, "Pedido creado");
        return pedido;
    }

    /** Reserva el stock en catalog (síncrona e idempotente por {@code pedidoId}); sin stock → 409, caído → 503. */
    private void reservar(Pedido pedido, Datos datos) {
        List<ReservaCatalogRequest.Item> items = datos.lineas().stream()
                .map(l -> new ReservaCatalogRequest.Item(l.varianteId(), l.cantidad())).toList();
        ReservaCatalogRequest peticion = new ReservaCatalogRequest(pedido.getId(), pedido.getExpiraEn(), items);
        try {
            dependencias.llamar((Supplier<Object>) () -> catalog.reservar(peticion));
        } catch (FeignException e) {
            if (e.status() == 409) {
                throw sinStockDeLaReserva(e, datos.lineas());
            }
            throw Dependencias.noDisponible(e);
        }
    }

    private FitstoreException sinStockDeLaReserva(FeignException e, List<LineaCotizada> lineas) {
        List<Map<String, Object>> faltantes = new ArrayList<>();
        try {
            JsonNode nodos = json.readTree(e.contentUTF8()).path("detalles").path("faltantes");
            for (JsonNode nodo : nodos) {
                faltantes.add(faltante(nodo.path("varianteId").asLong(), nodo.path("solicitado").asInt(),
                        nodo.path("disponible").asInt()));
            }
        } catch (RuntimeException ignorado) {
            // Si catalog no explica qué falta, se informa el conflicto sin detalle
        }
        List<LineaCotizada> afectadas = lineas.stream()
                .filter(l -> faltantes.stream().anyMatch(f -> f.get("varianteId").equals(l.varianteId()))).toList();
        return sinStock(nombres(afectadas.isEmpty() ? lineas : afectadas), faltantes);
    }

    private static ConflictoNegocioException sinStock(String nombres, List<Map<String, Object>> faltantes) {
        return new ConflictoNegocioException("SIN_STOCK", "Sin stock para: " + nombres, Map.<String, Object>of("faltantes", faltantes));
    }

    private static Map<String, Object> faltante(Long varianteId, int solicitado, int disponible) {
        Map<String, Object> faltante = new LinkedHashMap<>();
        faltante.put("varianteId", varianteId);
        faltante.put("solicitado", solicitado);
        faltante.put("disponible", disponible);
        return faltante;
    }

    private static String nombres(List<LineaCotizada> lineas) {
        return String.join(", ", lineas.stream()
                .map(l -> l.producto() + " " + l.talla() + " " + l.color()).toList());
    }

    private PedidoCreado eventoCreado(Pedido p) {
        return new PedidoCreado(p.getId(), p.getCodigo(), p.getClienteId(),
                PedidoMapper.destinatarioEvento(p.getDestinatario()), p.getModalidad(),
                PedidoMapper.direccionEvento(p.getDireccion()), PedidoMapper.itemsEvento(p), p.getSubtotal(),
                p.getDescuentoTotal(), p.getCostoEnvio(), p.getTotal(), p.getExpiraEn());
    }

    private static PedidoCreadoDto aCreado(Pedido p) {
        return new PedidoCreadoDto(p.getCodigo(), p.getTotal(), p.getExpiraEn());
    }

    private static ItemPedidoDto aItem(LineaCotizada l) {
        BigDecimal total = MontosPedido.totalItem(new Item(l.precioUnitario(), l.cantidad(), l.descuentoMonto()));
        return new ItemPedidoDto(l.varianteId(), l.productoId(), l.sku(), l.producto(), l.talla(), l.color(),
                l.imagenUrl(), l.precioUnitario(), l.cantidad(),
                l.descuentoPorcentaje() == null ? BigDecimal.ZERO : l.descuentoPorcentaje(), l.descuentoMonto(), total);
    }

    private EnvioDto envioDto(Datos datos) {
        CotizacionEnvio envio = datos.envio();
        if (envio == null) {
            return new EnvioDto(Modalidad.RECOJO, BigDecimal.ZERO.setScale(2), null, null, null, null, null, null,
                    datos.parametros().tiendaDireccion());
        }
        return new EnvioDto(datos.modalidad(), datos.montos().costoEnvio(), envio.plazoDiasMin(), envio.plazoDiasMax(),
                datos.modalidad() == Modalidad.AGENCIA ? datos.agenciaId() : null, envio.agenciaNombre(), null, null, null);
    }
}
