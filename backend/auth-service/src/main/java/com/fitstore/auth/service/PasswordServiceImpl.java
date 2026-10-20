package com.fitstore.auth.service;

import com.fitstore.auth.config.PropiedadesAuth;
import com.fitstore.auth.event.PasswordCambiado;
import com.fitstore.auth.event.PasswordResetSolicitado;
import com.fitstore.auth.exception.TokenInvalidoException;
import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.Cuenta;
import com.fitstore.auth.model.MotivoToken;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.model.TipoCuenta;
import com.fitstore.auth.model.TokenPassword;
import com.fitstore.auth.repository.TokenPasswordRepository;
import com.fitstore.auth.security.CuentaLookup;
import com.fitstore.auth.security.HashToken;
import com.fitstore.common.event.OutboxRegistrador;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordServiceImpl implements PasswordService {

    private final CuentaLookup cuentas;
    private final TokenPasswordRepository tokens;
    private final PasswordEncoder codificador;
    private final OutboxRegistrador outbox;
    private final PropiedadesAuth propiedades;
    private final Clock reloj;

    public PasswordServiceImpl(CuentaLookup cuentas, TokenPasswordRepository tokens, PasswordEncoder codificador,
            OutboxRegistrador outbox, PropiedadesAuth propiedades, Clock reloj) {
        this.cuentas = cuentas;
        this.tokens = tokens;
        this.codificador = codificador;
        this.outbox = outbox;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void solicitarRecuperacion(TipoCuenta tipo, String email) {
        cuentas.buscarPorEmail(tipo, Correo.normalizar(email))
                .filter(Cuenta::isActivo)
                .ifPresent(cuenta -> emitirEnlace(cuenta, MotivoToken.RECUPERACION, propiedades.tokenRecuperacionTtl()));
    }

    @Override
    @Transactional
    public void restablecer(String token, String nuevaPassword) {
        Instant ahora = reloj.instant();
        TokenPassword guardado = tokens.findByTokenHash(HashToken.sha256(token)).orElseThrow(TokenInvalidoException::new);
        if (guardado.getUsadoEn() != null || !guardado.getExpiraEn().isAfter(ahora)) {
            throw new TokenInvalidoException();
        }
        Cuenta cuenta = guardado.getCliente() != null ? guardado.getCliente() : guardado.getStaff();
        if (!cuenta.isActivo()) {
            throw new TokenInvalidoException();
        }
        cuenta.setPasswordHash(codificador.encode(nuevaPassword));
        // invalida todas las sesiones abiertas de la cuenta (invariante 8)
        cuenta.setCredencialesActualizadasEn(ahora.truncatedTo(ChronoUnit.MILLIS));
        guardado.setUsadoEn(ahora);
        outbox.registrar("PasswordCambiado", cuenta.getId().toString(),
                new PasswordCambiado(cuenta.getId(), cuenta.tipo(), cuenta.getEmail(), cuenta.getNombres()));
    }

    /** Invalida los enlaces vigentes de la cuenta, guarda el hash del nuevo y publica el evento con el enlace. */
    private void emitirEnlace(Cuenta cuenta, MotivoToken motivo, Duration vida) {
        Instant ahora = reloj.instant();
        invalidarVigentes(cuenta, ahora);

        String token = HashToken.generar();
        TokenPassword nuevo = new TokenPassword();
        if (cuenta instanceof Cliente cliente) {
            nuevo.setCliente(cliente);
        } else {
            nuevo.setStaff((Staff) cuenta);
        }
        nuevo.setTokenHash(HashToken.sha256(token));
        nuevo.setMotivo(motivo);
        nuevo.setExpiraEn(ahora.plus(vida));
        tokens.save(nuevo);

        outbox.registrar("PasswordResetSolicitado", cuenta.getId().toString(),
                new PasswordResetSolicitado(cuenta.getId(), cuenta.tipo(), cuenta.getEmail(), cuenta.getNombres(),
                        motivo, enlace(cuenta.tipo(), motivo, token), nuevo.getExpiraEn()));
    }

    private void invalidarVigentes(Cuenta cuenta, Instant ahora) {
        List<TokenPassword> vigentes = cuenta instanceof Cliente
                ? tokens.findByClienteIdAndUsadoEnIsNullAndExpiraEnAfter(cuenta.getId(), ahora)
                : tokens.findByStaffIdAndUsadoEnIsNullAndExpiraEnAfter(cuenta.getId(), ahora);
        vigentes.forEach(t -> t.setExpiraEn(ahora));
    }

    /** A3: clientes a la tienda; staff (recuperación e invitación) al backoffice. La invitación lleva {@code invitacion=1}. */
    private String enlace(TipoCuenta tipo, MotivoToken motivo, String token) {
        String raiz = tipo == TipoCuenta.CLIENTE ? propiedades.enlaces().storefront() : propiedades.enlaces().backoffice();
        String sinBarra = raiz.endsWith("/") ? raiz.substring(0, raiz.length() - 1) : raiz;
        return sinBarra + "/restablecer?token=" + token + (motivo == MotivoToken.INVITACION ? "&invitacion=1" : "");
    }
}
