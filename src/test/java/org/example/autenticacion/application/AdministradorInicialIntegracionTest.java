package org.example.autenticacion.application;

import org.example.compartido.JwtTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "peliculas.admin.email=admin.inicial@correo.com",
        "peliculas.admin.contrasena=ClaveInicial123"
})
class AdministradorInicialIntegracionTest {

    @Autowired
    private AutenticacionService servicio;

    @Autowired
    private JwtTokens tokens;

    @Test
    void alArrancarExisteUnAdministradorQuePuedeIniciarSesion() {
        String token = servicio.login("admin.inicial@correo.com", "ClaveInicial123");

        assertEquals("ADMIN", tokens.leer(token).rol());
    }
}
