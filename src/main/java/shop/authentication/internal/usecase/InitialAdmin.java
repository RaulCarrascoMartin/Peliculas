package shop.authentication.internal.usecase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * On startup creates the first admin from {@code peliculas.admin.email} and
 * {@code peliculas.admin.contrasena}. If neither is configured, it does nothing.
 */
@Component
public class InitialAdmin implements CommandLineRunner {

    private final CreateAdminIfMissing createAdmin;
    private final String email;
    private final String password;

    public InitialAdmin(CreateAdminIfMissing createAdmin,
                                @Value("${peliculas.admin.email:}") String email,
                                @Value("${peliculas.admin.contrasena:}") String password) {
        this.createAdmin = createAdmin;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "peliculas.admin.email and peliculas.admin.contrasena must be configured together");
        }
        createAdmin.execute(email, password);
    }
}
