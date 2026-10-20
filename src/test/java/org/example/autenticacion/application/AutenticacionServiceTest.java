package org.example.autenticacion.application;

import org.example.autenticacion.domain.Rol;
import org.example.compartido.ConflictoException;
import org.example.compartido.JwtTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutenticacionServiceTest {

    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder();
    private final JwtTokens tokens = new JwtTokens(
            "secreto-de-pruebas-con-al-menos-32-bytes-de-longitud", Duration.ofHours(1));

    private UsuarioRepositoryFalso repositorio;
    private AutenticacionService servicio;

    @BeforeEach
    void preparar() {
        repositorio = new UsuarioRepositoryFalso();
        servicio = new AutenticacionService(repositorio, codificador, tokens);
    }

    @Test
    void registraUnUsuarioConRolUsuarioYContrasenaCifrada() {
        var usuario = servicio.registrar("ana@correo.com", "Secreta123");

        assertEquals(Rol.USUARIO, usuario.getRol());
        assertNotEquals("Secreta123", usuario.getContrasena());
        assertTrue(codificador.matches("Secreta123", usuario.getContrasena()));
        assertEquals(usuario, repositorio.findByEmail("ana@correo.com").orElseThrow());
    }

    @Test
    void rechazaElRegistroDeUnEmailDuplicado() {
        servicio.registrar("ana@correo.com", "Secreta123");

        assertThrows(ConflictoException.class, () -> servicio.registrar("ana@correo.com", "Otra456"));
        assertEquals(1, repositorio.count());
    }

    @Test
    void loginCorrectoEmiteUnTokenConIdYRol() {
        var usuario = servicio.registrar("ana@correo.com", "Secreta123");

        var datos = tokens.leer(servicio.login("ana@correo.com", "Secreta123"));

        assertEquals(usuario.getId(), datos.id());
        assertEquals("USUARIO", datos.rol());
    }

    @Test
    void loginDeUnAdministradorLlevaSuRol() {
        var admin = servicio.registrar("root@correo.com", "Secreta123");
        admin.setRol(Rol.ADMIN);

        assertEquals("ADMIN", tokens.leer(servicio.login("root@correo.com", "Secreta123")).rol());
    }

    @Test
    void contrasenaIncorrectaYEmailInexistenteDanElMismoError() {
        servicio.registrar("ana@correo.com", "Secreta123");

        var contrasenaMala = assertThrows(BadCredentialsException.class,
                () -> servicio.login("ana@correo.com", "equivocada"));
        var emailMalo = assertThrows(BadCredentialsException.class,
                () -> servicio.login("nadie@correo.com", "Secreta123"));

        assertEquals(contrasenaMala.getMessage(), emailMalo.getMessage());
    }
}
