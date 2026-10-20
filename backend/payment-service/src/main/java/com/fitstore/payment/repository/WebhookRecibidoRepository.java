package com.fitstore.payment.repository;

import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.model.WebhookRecibido;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookRecibidoRepository extends JpaRepository<WebhookRecibido, UUID> {

    Optional<WebhookRecibido> findByProveedorAndEventoIdProveedor(Proveedor proveedor, String eventoIdProveedor);

    List<WebhookRecibido> findByProcesadoEnIsNull();
}
