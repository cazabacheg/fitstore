package com.fitstore.payment.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.api.FitstoreException;
import com.fitstore.common.security.UsuarioActual;
import com.fitstore.payment.dto.CargoRequest;
import com.fitstore.payment.dto.PaginaPago;
import com.fitstore.payment.dto.RespuestaResultadoCargo;
import com.fitstore.payment.dto.ResultadoCargoDto;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.service.BoletaService;
import com.fitstore.payment.service.CobroService;
import com.fitstore.payment.service.PagoService;
import com.fitstore.payment.service.ResultadoCobro;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Página de pago, cobro y boleta de la clienta dueña del pedido (sin lógica: delega en los servicios). */
@RestController
@RequestMapping(path = "/api/v1/pagos", produces = MediaType.APPLICATION_JSON_VALUE)
public class PagoController {

    private final PagoService pagos;
    private final CobroService cobros;
    private final BoletaService boletas;

    public PagoController(PagoService pagos, CobroService cobros, BoletaService boletas) {
        this.pagos = pagos;
        this.cobros = cobros;
        this.boletas = boletas;
    }

    @GetMapping("/{codigoPedido}")
    public ApiResponse<PaginaPago> obtenerPaginaPago(@PathVariable String codigoPedido, Authentication autenticacion) {
        return ApiResponse.ok(pagos.obtenerPagina(codigoPedido, clienteId(autenticacion)));
    }

    @PostMapping("/{codigoPedido}/cargos")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pago aprobado.",
            content = @io.swagger.v3.oas.annotations.media.Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = RespuestaResultadoCargo.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202",
            description = "La pasarela no respondió a tiempo; la página consulta el estado cada 3 s (P4).",
            content = @io.swagger.v3.oas.annotations.media.Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = RespuestaResultadoCargo.class)))
    public ResponseEntity<ApiResponse<ResultadoCargoDto>> cobrarPedido(@PathVariable String codigoPedido,
            @RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody CargoRequest solicitud,
            Authentication autenticacion) {
        ResultadoCobro cobro = cobros.cobrar(codigoPedido, clienteId(autenticacion), solicitud, idempotencyKey);
        Transaccion t = cobro.transaccion();
        ResultadoCargoDto dto = new ResultadoCargoDto(t.getEstado(), cobro.intencion().getCodigoPedido(), t.getMonto(),
                t.getProveedor(), t.getMedio(), cobro.descripcionMedio());
        return switch (t.getEstado()) {
            case APROBADA -> ResponseEntity.ok(ApiResponse.ok("¡Pago aprobado!", dto));
            case PROCESANDO -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.ok("Estamos confirmando tu pago", dto));
            // El rechazo se responde fuera de la transacción del cobro, para no revertir la transacción RECHAZADA
            case RECHAZADA, ERROR -> throw new FitstoreException(HttpStatus.PAYMENT_REQUIRED, "PAGO_RECHAZADO",
                    t.getMensaje() == null ? "Pago rechazado" : t.getMensaje());
        };
    }

    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Boleta simulada en PDF.",
            content = @io.swagger.v3.oas.annotations.media.Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "binary")))
    @GetMapping(path = "/{codigoPedido}/boleta", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarMiBoleta(@PathVariable String codigoPedido, Authentication autenticacion) {
        return BoletaRespuestas.pdf(boletas.descargarDeLaClienta(codigoPedido, clienteId(autenticacion)));
    }

    /** El id de la clienta sale del token; un token de staff no puede pagar. */
    static UUID clienteId(Authentication autenticacion) {
        UsuarioActual usuario = UsuarioActual.de(autenticacion);
        if (!"CLIENTE".equals(usuario.tipo())) {
            throw new AccessDeniedException("Solo las clientas pagan pedidos");
        }
        return UUID.fromString(usuario.id());
    }
}
