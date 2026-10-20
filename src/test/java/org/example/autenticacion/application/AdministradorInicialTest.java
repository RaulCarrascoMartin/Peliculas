package org.example.autenticacion.application;

import org.example.autenticacion.domain.Rol;
import org.example.compartido.JwtTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdministradorInicialTest {

    private UsuarioRepositoryFalso repositorio;
    private AutenticacionService servicio;

    @BeforeEach
    void preparar() {
        repositorio = new UsuarioRepositoryFalso();
        servicio = new AutenticacionService(repositorio, new BCryptPasswordEncoder(), new JwtTokens(
                "secreto-de-pruebas-con-al-menos-32-bytes-de-longitud", Duration.ofHours(1)));
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
