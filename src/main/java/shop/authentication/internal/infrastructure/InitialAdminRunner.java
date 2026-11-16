package shop.authentication.internal.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import shop.authentication.internal.domain.service.AuthService;

/**
 * On startup hands the configured {@code peliculas.admin.email} and {@code peliculas.admin.contrasena} to the
 * service, which decides whether an initial admin has to be created.
 */
@Component
public class InitialAdminRunner implements CommandLineRunner {

    private final AuthService authService;
    private final String email;
    private final String password;

    public InitialAdminRunner(AuthService authService,
                              @Value("${peliculas.admin.email:}") String email,
                              @Value("${peliculas.admin.contrasena:}") String password) {
        this.authService = authService;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        authService.ensureInitialAdmin(email, password);
    }
}
