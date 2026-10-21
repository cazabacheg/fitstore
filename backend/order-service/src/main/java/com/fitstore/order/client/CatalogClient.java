package com.fitstore.order.client;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.order.client.dto.CotizacionCatalog;
import com.fitstore.order.client.dto.CotizarCatalogRequest;
import com.fitstore.order.client.dto.ReservaCatalogRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * catalog-service: cotización del carrito (pública) y reserva de stock del checkout (interna, con el JWT del cliente
 * propagado). Contrato: {@code contracts/openapi/catalog-service.yaml}.
 */
@FeignClient(name = "catalog-service", url = "${fitstore.clientes.catalog.url:}")
public interface CatalogClient {

    @PostMapping("/api/v1/precios/cotizar")
    ApiResponse<CotizacionCatalog> cotizar(@RequestBody CotizarCatalogRequest peticion);

    /** Idempotente por {@code pedidoId}; sin stock responde 409 {@code SIN_STOCK} con {@code detalles.faltantes}. */
    @PostMapping("/internal/v1/inventario/reservas")
    ApiResponse<Object> reservar(@RequestBody ReservaCatalogRequest peticion);
}
