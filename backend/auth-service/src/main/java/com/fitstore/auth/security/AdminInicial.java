package com.fitstore.auth.security;

import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.repository.StaffRepository;
import com.fitstore.auth.service.Correo;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Crea el primer ADMIN al arrancar desde {@code FITSTORE_ADMIN_EMAIL} y {@code FITSTORE_ADMIN_PASSWORD} (D6, CA-1-38).
 * Sin contraseñas ni hashes en las migraciones ni en el repositorio. Si ya existe algún ADMIN, no hace nada.
 */
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminInicial.class);

    private final StaffRepository personal;
    private final PasswordEncoder codificador;
    private final Clock reloj;
    private final String email;
    private final String password;

    public AdminInicial(StaffRepository personal, PasswordEncoder codificador, Clock reloj,
            @Value("${FITSTORE_ADMIN_EMAIL:}") String email, @Value("${FITSTORE_ADMIN_PASSWORD:}") String password) {
        this.personal = personal;
        this.codificador = codificador;
        this.reloj = reloj;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            LOG.info("Sin FITSTORE_ADMIN_EMAIL y FITSTORE_ADMIN_PASSWORD: no se crea el admin inicial");
            return;
        }
        if (personal.existsByRol(RolStaff.ADMIN)) {
            return;
        }
        String correo = Correo.normalizar(email);
        if (personal.existsByEmailIgnoreCase(correo)) {
            LOG.warn("El correo del admin inicial ya pertenece a un miembro del staff que no es ADMIN: no se crea la cuenta");
            return;
        }
        Staff admin = new Staff();
        admin.setEmail(correo);
        admin.setNombres("Administrador");
        admin.setApellidos("FitStore");
        admin.setRol(RolStaff.ADMIN);
        admin.setPasswordHash(codificador.encode(password));
        admin.setActivo(true);
        admin.setCredencialesActualizadasEn(reloj.instant().truncatedTo(ChronoUnit.MILLIS));
        personal.save(admin);
        LOG.info("Admin inicial creado: {}", correo);
    }
}
