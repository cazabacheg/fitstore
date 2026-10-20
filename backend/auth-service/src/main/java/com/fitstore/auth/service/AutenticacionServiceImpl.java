package com.fitstore.auth.service;

import com.fitstore.auth.model.TipoCuenta;
import com.fitstore.auth.security.CuentaLookup;
import com.fitstore.auth.security.RegistroIntentosLogin;
import com.fitstore.auth.security.SesionEmitida;
import com.fitstore.auth.security.TokenService;
import java.time.Clock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticacionServiceImpl implements AutenticacionService {

    public AutenticacionServiceImpl(CuentaLookup cuentas, RegistroIntentosLogin intentos, PasswordEncoder codificador,
            TokenService tokens, Clock reloj) {
    }

    @Override
    public SesionEmitida iniciarSesion(TipoCuenta tipo, String email, String password) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public SesionEmitida refrescar(String refreshToken) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public void cerrarSesion(String refreshToken) {
        throw new UnsupportedOperationException("pendiente");
    }
}
