package com.fitstore.catalog.inventario.service;

import com.fitstore.catalog.event.ItemCantidad;
import com.fitstore.catalog.inventario.dto.ReservaRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ReservaServiceImpl implements ReservaService {

    @Override
    public Resultado reservar(ReservaRequest peticion) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public void confirmarPago(UUID pedidoId, String codigoPedido, List<ItemCantidad> items) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public void cancelar(UUID pedidoId, String codigoPedido) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public void expirar(UUID pedidoId) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public int liberarVencidas() {
        throw new UnsupportedOperationException("pendiente");
    }
}
