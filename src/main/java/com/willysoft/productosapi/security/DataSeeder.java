package com.willysoft.productosapi.security;

import com.willysoft.productosapi.parametro.Parametro;
import com.willysoft.productosapi.parametro.ParametroRepository;
import com.willysoft.productosapi.parametro.ParametroService;
import com.willysoft.productosapi.user.Role;
import com.willysoft.productosapi.user.User;
import com.willysoft.productosapi.user.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el primer ADMIN al arrancar si todavía no existe ningún usuario.
 * Credenciales configurables por variables de entorno (ver application.properties).
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final ParametroRepository parametroRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.admin.nombre:Administrador}")
    private String adminNombre;

    @Value("${app.seed.admin.email:admin@willysoft.com}")
    private String adminEmail;

    @Value("${app.seed.admin.password:Admin123!}")
    private String adminPassword;

    @Value("${app.seed.admin.recovery-email:}")
    private String adminRecoveryEmail;

    @Override
    public void run(String... args) {
        seedParametros();
        seedAdmin();
    }

    private void seedParametros() {
        if (!parametroRepository.existsById(ParametroService.DOLAR)) {
            parametroRepository.save(Parametro.builder()
                    .clave(ParametroService.DOLAR)
                    .valor("1.00")
                    .descripcion("Cotización del dólar (ARS por USD)")
                    .fechaActualizacion(LocalDateTime.now())
                    .actualizadoPor("seeder")
                    .build());
            log.warn("==> Parámetro DOLAR inicial creado con valor 1.00 (actualizalo en /admin/parametros)");
        }
        if (!parametroRepository.existsById(ParametroService.STOCK_MINIMO)) {
            parametroRepository.save(Parametro.builder()
                    .clave(ParametroService.STOCK_MINIMO)
                    .valor("0")
                    .descripcion("Stock mínimo global: avisa si el stock baja a este valor (0 = sin aviso)")
                    .fechaActualizacion(LocalDateTime.now())
                    .actualizadoPor("seeder")
                    .build());
            log.warn("==> Parámetro STOCK_MINIMO inicial creado con valor 0 (configuralo en /admin/parametros)");
        }
    }

    private void seedAdmin() {
        if (!seedEnabled) {
            return;
        }
        if (userRepository.count() > 0) {
            return;
        }
        User admin = User.builder()
                .nombre(adminNombre)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .rol(Role.ADMIN)
                .emailRecuperacion(adminRecoveryEmail != null && !adminRecoveryEmail.isBlank()
                        ? adminRecoveryEmail.trim() : null)
                .activo(true)
                .bloqueado(false)
                .creadoPor("seeder")
                .build();
        userRepository.save(admin);
        log.warn("==> Usuario ADMIN inicial creado: {} (cambia la contraseña en producción)", adminEmail);
    }
}
