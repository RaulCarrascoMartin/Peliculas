package org.example.autenticacion.internal.usecase;

import org.example.autenticacion.internal.domain.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdministradorInicialTest {

    private UsuarioRepositoryFalso repositorio;
    private CrearAdministradorSiNoExiste servicio;

    @BeforeEach
    void preparar() {
        repositorio = new UsuarioRepositoryFalso();
        servicio = new CrearAdministradorSiNoExiste(repositorio, new BCryptPasswordEncoder());
    }

    @Test
    void sinConfiguracionNoCreaNingunUsuario() throws Exception {
        new AdministradorInicial(servicio, "", "").run();

        assertEquals(0, repositorio.count());
    }

    @Test
    void conEmailYContrasenaCreaElAdministrador() throws Exception {
        new AdministradorInicial(servicio, "root@correo.com", "Secreta123").run();

        assertEquals(Rol.ADMIN, repositorio.findByEmail("root@correo.com").orElseThrow().getRol());
    }

    @Test
    void emailSinContrasenaEsUnErrorDeConfiguracion() {
        assertThrows(IllegalStateException.class,
                () -> new AdministradorInicial(servicio, "root@correo.com", "").run());
        assertEquals(0, repositorio.count());
    }
}
