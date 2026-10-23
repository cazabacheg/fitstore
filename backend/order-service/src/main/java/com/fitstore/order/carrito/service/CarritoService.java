package com.fitstore.order.carrito.service;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.FitstoreException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.order.carrito.dto.AvisoTramoDto;
import com.fitstore.order.carrito.dto.CarritoDto;
import com.fitstore.order.carrito.dto.LineaCarritoDto;
import com.fitstore.order.carrito.model.CarritoId;
import com.fitstore.order.carrito.repository.CarritoRepository;
import com.fitstore.order.client.CatalogClient;
import com.fitstore.order.client.Dependencias;
import com.fitstore.order.client.dto.CotizacionCatalog;
import com.fitstore.order.client.dto.CotizacionCatalog.LineaCotizada;
import com.fitstore.order.client.dto.CotizarCatalogRequest;
import com.fitstore.order.config.PropiedadesOrder;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * Carrito anónimo y de cliente (micro 4.2, HU-4-01). Solo guarda {@code varianteId → cantidad} en Redis (D1); los
 * precios se piden a catalog cada vez que se muestra, y la última cotización se conserva para cuando catalog no
 * responde (D2).
 */
@Service
public class CarritoService {

    public static final String LIMITE_CARRITO = "LIMITE_CARRITO";
    public static final String STOCK_INSUFICIENTE = "STOCK_INSUFICIENTE";

    private final CarritoRepository carritos;
    private final CatalogClient catalog;
    private final Dependencias dependencias;
    private final PropiedadesOrder propiedades;
    private final JsonMapper json;

    public CarritoService(CarritoRepository carritos, CatalogClient catalog, Dependencias dependencias,
            PropiedadesOrder propiedades, JsonMapper json) {
        this.carritos = carritos;
        this.catalog = catalog;
        this.dependencias = dependencias;
        this.propiedades = propiedades;
        this.json = json;
    }

    /** Carrito con la cotización actual; si catalog no responde, con la última y {@code preciosPorConfirmar}. */
    public CarritoDto ver(CarritoId carrito) {
        Map<Long, Integer> lineas = ordenadas(carritos.obtenerLineas(carrito));
        if (lineas.isEmpty()) {
            return CarritoDto.vacio();
        }
        try {
            CotizacionCatalog cotizacion = cotizar(lineas);
            carritos.guardarCotizacion(carrito, json.writeValueAsString(cotizacion));
            return armar(lineas, cotizacion, false);
        } catch (FitstoreException e) {
            if (e.estado() != HttpStatus.SERVICE_UNAVAILABLE) {
                throw e;
            }
            Optional<CotizacionCatalog> ultima = ultimaCotizacion(carrito);
            if (ultima.isEmpty()) {
                throw e;
            }
            return armar(lineas, ultima.get(), true);
        }
    }

    /** Suma la cantidad a la línea (CA-4-01) validando límites (CA-4-05) y stock (CA-4-04). */
    public CarritoDto agregar(CarritoId carrito, long varianteId, int cantidad) {
        Map<Long, Integer> lineas = carritos.obtenerLineas(carrito);
        if (!lineas.containsKey(varianteId) && lineas.size() >= propiedades.carrito().maxLineas()) {
            throw limite("Tu carrito acepta como máximo " + propiedades.carrito().maxLineas() + " productos distintos");
        }
        int nueva = lineas.getOrDefault(varianteId, 0) + cantidad;
        fijar(carrito, varianteId, nueva);
        return ver(carrito);
    }

    /** Cambia la cantidad de una línea existente. */
    public CarritoDto cambiarCantidad(CarritoId carrito, long varianteId, int cantidad) {
        if (!carritos.obtenerLineas(carrito).containsKey(varianteId)) {
            throw new RecursoNoEncontradoException("Ese producto no está en tu carrito");
        }
        fijar(carrito, varianteId, cantidad);
        return ver(carrito);
    }

    public CarritoDto quitar(CarritoId carrito, long varianteId) {
        if (!carritos.obtenerLineas(carrito).containsKey(varianteId)) {
            throw new RecursoNoEncontradoException("Ese producto no está en tu carrito");
        }
        carritos.quitarLinea(carrito, varianteId);
        return ver(carrito);
    }

    /**
     * Fusión al iniciar sesión (CA-4-10): suma lo del carrito anónimo al del cliente, borra el anónimo y devuelve el
     * carrito resultante. Una suma que pasa del tope por línea queda en el tope y se avisa. Sin carrito anónimo
     * ({@code null}) solo devuelve el carrito del cliente, así que repetir la llamada no cambia nada.
     */
    public CarritoDto fusionar(CarritoId cliente, CarritoId anonimo) {
        if (anonimo == null) {
            return ver(cliente);
        }
        int tope = propiedades.carrito().maxUnidadesLinea();
        Map<Long, Integer> resultado = carritos.fusionar(anonimo, cliente);
        List<Long> ajustadas = new java.util.ArrayList<>();
        resultado.forEach((variante, cantidad) -> {
            if (cantidad > tope) {
                carritos.establecerLinea(cliente, variante, tope);
                ajustadas.add(variante);
            }
        });
        CarritoDto carrito = ver(cliente);
        if (ajustadas.isEmpty()) {
            return carrito;
        }
        List<String> avisos = ajustadas.stream()
                .map(v -> "La cantidad de " + carrito.lineas().stream().filter(l -> v.equals(l.varianteId()))
                        .map(LineaCarritoDto::producto).findFirst().orElse("un producto") + " se ajustó a " + tope)
                .toList();
        return new CarritoDto(carrito.lineas(), carrito.avisosTramo(), carrito.subtotal(), carrito.descuentoTotal(),
                carrito.total(), carrito.cantidadTotal(), carrito.preciosPorConfirmar(), carrito.puedeIrAlCheckout(),
                avisos);
    }

    public CarritoDto vaciar(CarritoId carrito) {
        carritos.vaciar(carrito);
        return CarritoDto.vacio();
    }

    // ---------- internos ----------

    private void fijar(CarritoId carrito, long varianteId, int cantidad) {
        int tope = propiedades.carrito().maxUnidadesLinea();
        if (cantidad > tope) {
            throw limite("Máximo " + tope + " unidades por producto");
        }
        verificarStock(varianteId, cantidad);
        carritos.establecerLinea(carrito, varianteId, cantidad);
    }

    /** Pregunta a catalog por esa variante con la cantidad pedida; si no alcanza, 409 y el carrito no cambia. */
    private void verificarStock(long varianteId, int cantidad) {
        CotizacionCatalog cotizacion = cotizar(Map.of(varianteId, cantidad));
        LineaCotizada linea = cotizacion.lineas().stream()
                .filter(l -> l.varianteId() != null && l.varianteId() == varianteId)
                .findFirst().orElse(cotizacion.lineas().isEmpty() ? null : cotizacion.lineas().get(0));
        if (linea == null) {
            return;
        }
        if (CotizacionCatalog.NO_DISPONIBLE.equals(linea.estado())) {
            throw new ConflictoNegocioException("PRODUCTO_NO_DISPONIBLE", "Este producto ya no está disponible");
        }
        if (CotizacionCatalog.STOCK_INSUFICIENTE.equals(linea.estado())) {
            int disponible = linea.disponible() == null ? 0 : linea.disponible();
            throw new ConflictoNegocioException(STOCK_INSUFICIENTE, mensajeDeStock(disponible),
                    Map.of("disponible", disponible));
        }
    }

    static String mensajeDeStock(int disponible) {
        if (disponible <= 0) {
            return "Este producto está agotado";
        }
        return disponible == 1 ? "Solo queda 1 unidad" : "Solo quedan " + disponible + " unidades";
    }

    private static FitstoreException limite(String mensaje) {
        return new FitstoreException(HttpStatus.BAD_REQUEST, LIMITE_CARRITO, mensaje);
    }

    private CotizacionCatalog cotizar(Map<Long, Integer> lineas) {
        List<CotizarCatalogRequest.Linea> pedidas = lineas.entrySet().stream()
                .map(e -> new CotizarCatalogRequest.Linea(e.getKey(), e.getValue())).toList();
        var respuesta = dependencias.llamar(() -> catalog.cotizar(new CotizarCatalogRequest(pedidas)));
        if (respuesta == null || respuesta.data() == null) {
            throw Dependencias.noDisponible(null);
        }
        return respuesta.data();
    }

    private Optional<CotizacionCatalog> ultimaCotizacion(CarritoId carrito) {
        return carritos.leerCotizacion(carrito).map(texto -> json.readValue(texto, CotizacionCatalog.class));
    }

    /** Orden estable por variante: Redis no garantiza el orden de un hash. */
    private static Map<Long, Integer> ordenadas(Map<Long, Integer> lineas) {
        return new TreeMap<>(lineas);
    }

    private CarritoDto armar(Map<Long, Integer> lineas, CotizacionCatalog cotizacion, boolean porConfirmar) {
        List<LineaCarritoDto> filas = cotizacion.lineas().stream()
                .sorted(Comparator.comparing(LineaCotizada::varianteId))
                .map(l -> new LineaCarritoDto(l.varianteId(), l.productoId(), l.slug(), l.sku(), l.producto(),
                        l.talla(), l.color(), l.imagenUrl(), l.precioUnitario(), l.cantidad(),
                        l.descuentoPorcentaje(), l.descuentoMonto(), l.total(), l.estado(), l.disponible(), l.unidadesComprables()))
                .toList();
        List<AvisoTramoDto> avisos = cotizacion.productos() == null ? List.of() : cotizacion.productos().stream()
                .map(p -> new AvisoTramoDto(p.productoId(), p.nombre(), p.unidades(),
                        p.tramoAlcanzado() == null ? null
                                : new AvisoTramoDto.Tramo(p.tramoAlcanzado().unidadesMinimas(), p.tramoAlcanzado().porcentaje()),
                        p.siguienteTramo() == null ? null
                                : new AvisoTramoDto.SiguienteTramo(p.siguienteTramo().unidadesMinimas(),
                                        p.siguienteTramo().porcentaje(), p.siguienteTramo().faltan())))
                .toList();
        boolean hayNoDisponible = filas.stream().anyMatch(l -> CotizacionCatalog.NO_DISPONIBLE.equals(l.estado()));
        int cantidadTotal = lineas.values().stream().mapToInt(Integer::intValue).sum();
        BigDecimal cero = BigDecimal.ZERO.setScale(2);
        return new CarritoDto(filas, avisos,
                cotizacion.subtotal() == null ? cero : cotizacion.subtotal(),
                cotizacion.descuentoTotal() == null ? cero : cotizacion.descuentoTotal(),
                cotizacion.total() == null ? cero : cotizacion.total(),
                cantidadTotal, porConfirmar, !porConfirmar && !hayNoDisponible && !filas.isEmpty(), List.of());
    }
}
