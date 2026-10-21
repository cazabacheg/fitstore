package com.fitstore.auth.security;

import com.fitstore.auth.model.Cuenta;
import com.fitstore.auth.model.TipoCuenta;
import com.fitstore.auth.repository.ClienteRepository;
import com.fitstore.auth.repository.StaffRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CuentaLookupJpa implements CuentaLookup {

    private final ClienteRepository clientes;
    private final StaffRepository personal;

    public CuentaLookupJpa(ClienteRepository clientes, StaffRepository personal) {
        this.clientes = clientes;
        this.personal = personal;
    }

    @Override
    public Optional<Cuenta> buscar(TipoCuenta tipo, UUID id) {
        return switch (tipo) {
            case CLIENTE -> clientes.findById(id).map(Cuenta.class::cast);
            case STAFF -> personal.findById(id).map(Cuenta.class::cast);
        };
    }

    @Override
    public Optional<Cuenta> buscarPorEmail(TipoCuenta tipo, String email) {
        return switch (tipo) {
            case CLIENTE -> clientes.findByEmailIgnoreCase(email).map(Cuenta.class::cast);
            case STAFF -> personal.findByEmailIgnoreCase(email).map(Cuenta.class::cast);
        };
    }
}
