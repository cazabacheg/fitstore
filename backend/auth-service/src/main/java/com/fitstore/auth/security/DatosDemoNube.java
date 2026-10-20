package com.fitstore.auth.security;

import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.repository.ClienteRepository;
import com.fitstore.auth.repository.StaffRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Con el perfil {@code demo} (nube) las cuentas de demostración no conservan la contraseña pública de desarrollo
 * (A7, CA-1-39): al arrancar se les fija la de {@code DEMO_PASSWORD} o, si falta, se desactivan.
 */
@Profile("demo")
@Component
public class DatosDemoNube implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosDemoNube.class);

    private final ClienteRepository clientes;
    private final StaffRepository personal;
    private final PasswordEncoder codificador;
    private final Clock reloj;
    private final String passwordDemo;

    public DatosDemoNube(ClienteRepository clientes, StaffRepository personal, PasswordEncoder codificador, Clock reloj,
            @Value("${DEMO_PASSWORD:}") String passwordDemo) {
        this.clientes = clientes;
        this.personal = personal;
        this.codificador = codificador;
        this.reloj = reloj;
        this.passwordDemo = passwordDemo;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        Instant ahora = reloj.instant().truncatedTo(ChronoUnit.MILLIS);
        boolean hayContrasena = StringUtils.hasText(passwordDemo);
        // un solo hash para todas: el costo de BCrypt se paga una vez
        String hash = hayContrasena ? codificador.encode(passwordDemo) : null;

        List<Cliente> demoClientes = clientes.findAllById(CuentasDemo.CLIENTES);
        demoClientes.forEach(c -> ajustar(c, hash, ahora));
        clientes.saveAll(demoClientes);

        List<Staff> demoStaff = personal.findAllById(CuentasDemo.STAFF);
        demoStaff.forEach(s -> ajustar(s, hash, ahora));
        personal.saveAll(demoStaff);

        if (hayContrasena) {
            LOG.info("Cuentas de demostración con la contraseña de DEMO_PASSWORD ({} clientes, {} staff)", demoClientes.size(), demoStaff.size());
        } else {
            LOG.warn("DEMO_PASSWORD no está definida: se desactivaron las cuentas de demostración ({} clientes, {} staff)",
                    demoClientes.size(), demoStaff.size());
        }
    }

    private static void ajustar(com.fitstore.auth.model.Cuenta cuenta, String hash, Instant ahora) {
        if (hash != null) {
            cuenta.setPasswordHash(hash);
            cuenta.setActivo(true);
        } else {
            cuenta.setActivo(false);
        }
        // cierra cualquier sesión abierta con la contraseña anterior
        cuenta.setCredencialesActualizadasEn(ahora);
    }
}
