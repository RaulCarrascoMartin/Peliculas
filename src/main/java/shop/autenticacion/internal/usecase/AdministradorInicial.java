package shop.autenticacion.internal.usecase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Al arrancar crea el primer administrador a partir de {@code peliculas.admin.email} y
 * {@code peliculas.admin.contrasena}. Si no se configura ninguno de los dos, no hace nada.
 */
@Component
public class AdministradorInicial implements CommandLineRunner {

    private final CrearAdministradorSiNoExiste crearAdministrador;
    private final String email;
    private final String contrasena;

    public AdministradorInicial(CrearAdministradorSiNoExiste crearAdministrador,
                                @Value("${peliculas.admin.email:}") String email,
                                @Value("${peliculas.admin.contrasena:}") String contrasena) {
        this.crearAdministrador = crearAdministrador;
        this.email = email;
        this.contrasena = contrasena;
    }

    @Override
    public void run(String... args) {
        if (email.isBlank() && contrasena.isBlank()) {
            return;
        }
        if (email.isBlank() || contrasena.isBlank()) {
            throw new IllegalStateException(
                    "Hay que configurar a la vez peliculas.admin.email y peliculas.admin.contrasena");
        }
        crearAdministrador.ejecutar(email, contrasena);
    }
}
