package com.fitstore.auth.service;

import com.fitstore.auth.exception.CredencialesInvalidasException;
import com.fitstore.auth.exception.CuentaBloqueadaException;
import com.fitstore.auth.model.Cuenta;
import com.fitstore.auth.model.TipoCuenta;
import com.fitstore.auth.security.CuentaLookup;
import com.fitstore.auth.security.RegistroIntentosLogin;
import com.fitstore.auth.security.SesionEmitida;
import com.fitstore.auth.security.TokenService;
import java.time.Clock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacionServiceImpl implements AutenticacionService {

    private final CuentaLookup cuentas;
    private final RegistroIntentosLogin intentos;
    private final PasswordEncoder codificador;
    private final TokenService tokens;
    private final Clock reloj;
    /** Hash de nadie: se compara cuando la cuenta no existe, para que el tiempo de respuesta no la delate. */
    private final String hashFicticio;

    public AutenticacionServiceImpl(CuentaLookup cuentas, RegistroIntentosLogin intentos, PasswordEncoder codificador,
            TokenService tokens, Clock reloj) {
        this.cuentas = cuentas;
        this.intentos = intentos;
        this.codificador = codificador;
        this.tokens = tokens;
        this.reloj = reloj;
        this.hashFicticio = codificador.encode("contrasena-ficticia-de-nadie");
    }

    @Override
    @Transactional
    public SesionEmitida iniciarSesion(TipoCuenta tipo, String email, String password) {
        String correo = Correo.normalizar(email);
        if (intentos.estaBloqueado(tipo, correo)) {
            throw new CuentaBloqueadaException();
        }
        Cuenta cuenta = cuentas.buscarPorEmail(tipo, correo).orElse(null);
        boolean tieneContrasena = cuenta != null && cuenta.getPasswordHash() != null;
        // siempre se ejecuta un BCrypt, exista o no la cuenta (CA-1-05)
        boolean coincide = codificador.matches(password, tieneContrasena ? cuenta.getPasswordHash() : hashFicticio);
        if (!tieneContrasena || !coincide || !cuenta.isActivo()) {
            intentos.registrarFallo(tipo, correo);
            throw new CredencialesInvalidasException();
        }
        intentos.limpiar(tipo, correo);
        cuenta.setUltimoLoginEn(reloj.instant());
        return new SesionEmitida(cuenta, tokens.emitir(cuenta));
    }

    @Override
    public SesionEmitida refrescar(String refreshToken) {
        return tokens.refrescar(refreshToken);
    }

    @Override
    public void cerrarSesion(String refreshToken) {
        tokens.revocar(refreshToken);
    }
}
