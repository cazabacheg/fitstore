package com.fitstore.auth.service;

import com.fitstore.auth.dto.request.RegistroRequest;
import com.fitstore.auth.security.SesionEmitida;
import com.fitstore.auth.event.ClienteRegistrado;
import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.Consentimiento;
import com.fitstore.auth.model.TipoConsentimiento;
import com.fitstore.auth.repository.ClienteRepository;
import com.fitstore.auth.repository.ConsentimientoRepository;
import com.fitstore.auth.security.TokenService;
import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.event.OutboxRegistrador;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroServiceImpl implements RegistroService {

    private final ClienteRepository clientes;
    private final ConsentimientoRepository consentimientos;
    private final PasswordEncoder codificador;
    private final TokenService tokens;
    private final OutboxRegistrador outbox;
    private final Clock reloj;

    public RegistroServiceImpl(ClienteRepository clientes, ConsentimientoRepository consentimientos,
            PasswordEncoder codificador, TokenService tokens, OutboxRegistrador outbox, Clock reloj) {
        this.clientes = clientes;
        this.consentimientos = consentimientos;
        this.codificador = codificador;
        this.tokens = tokens;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public SesionEmitida registrar(RegistroRequest solicitud, String ip) {
        String email = Correo.normalizar(solicitud.email());
        if (clientes.existsByEmailIgnoreCase(email)) {
            throw new ConflictoNegocioException("CORREO_DUPLICADO", "El correo ya está registrado");
        }
        if (clientes.existsByDni(solicitud.dni())) {
            throw new ConflictoNegocioException("DNI_DUPLICADO", "El DNI ya está registrado");
        }

        Instant ahora = reloj.instant().truncatedTo(ChronoUnit.MILLIS);
        Cliente cliente = new Cliente();
        cliente.setEmail(email);
        cliente.setPasswordHash(codificador.encode(solicitud.password()));
        cliente.setNombres(solicitud.nombres().trim());
        cliente.setApellidos(solicitud.apellidos().trim());
        cliente.setDni(solicitud.dni());
        cliente.setTelefono(solicitud.telefono());
        cliente.setActivo(true);
        cliente.setCredencialesActualizadasEn(ahora);
        cliente.setUltimoLoginEn(ahora);
        clientes.save(cliente);

        consentimientos.save(consentimiento(cliente, TipoConsentimiento.TERMINOS, solicitud.versionTerminos(), ahora, ip));
        consentimientos.save(consentimiento(cliente, TipoConsentimiento.PRIVACIDAD, solicitud.versionPrivacidad(), ahora, ip));
        outbox.registrar("ClienteRegistrado", cliente.getId().toString(),
                new ClienteRegistrado(cliente.getId(), cliente.getEmail(), cliente.getNombres()));

        return new SesionEmitida(cliente, tokens.emitir(cliente));
    }

    private static Consentimiento consentimiento(Cliente cliente, TipoConsentimiento tipo, Integer version, Instant ahora, String ip) {
        Consentimiento c = new Consentimiento();
        c.setCliente(cliente);
        c.setTipo(tipo);
        c.setVersionPolitica(String.valueOf(version));
        c.setAceptadoEn(ahora);
        c.setIp(ip);
        return c;
    }
}
