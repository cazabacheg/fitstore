package com.fitstore.auth.security;

import com.fitstore.auth.repository.ClienteRepository;
import com.fitstore.auth.repository.StaffRepository;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Con el perfil demo (nube) fija la contraseña de las cuentas de demostración desde {@code DEMO_PASSWORD} (A7, CA-1-39). */
@Profile("demo")
@Component
public class DatosDemoNube implements ApplicationRunner {

    public DatosDemoNube(ClienteRepository clientes, StaffRepository personal, PasswordEncoder codificador, Clock reloj,
            @Value("${DEMO_PASSWORD:}") String passwordDemo) {
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        // pendiente
    }
}
