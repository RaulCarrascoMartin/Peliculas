package org.example.autenticacion.internal.usecase;

import org.example.autenticacion.internal.domain.Rol;
import org.example.compartido.error.ConflictoException;
import org.example.compartido.seguridad.JwtTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutenticacionCasosDeUsoTest {

    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder();
    private final JwtTokens tokens = new JwtTokens(
            "secreto-de-pruebas-con-al-menos-32-bytes-de-longitud", Duration.ofHours(1));

    private UsuarioRepositoryFalso repositorio;
    private RegistrarUsuario registrar;
    private IniciarSesion iniciarSesion;
    private CrearAdministradorSiNoExiste crearAdministrador;

    @BeforeEach
    void preparar() {
        repositorio = new UsuarioRepositoryFalso();
        registrar = new RegistrarUsuario(repositorio, codificador);
        iniciarSesion = new IniciarSesion(repositorio, codificador, tokens);
        crearAdministrador = new CrearAdministradorSiNoExiste(repositorio, codificador);
    }

    @Test
    void registraUnUsuarioConRolUsuarioYContrasenaCifrada() {
        var usuario = registrar.ejecutar("ana@correo.com", "Secreta123");

        var guardado = repositorio.findByEmail("ana@correo.com").orElseThrow();
        assertEquals("USUARIO", usuario.rol());
        assertEquals(guardado.getId(), usuario.id());
        assertNotEquals("Secreta123", guardado.getContrasena());
        assertTrue(codificador.matches("Secreta123", guardado.getContrasena()));
    }

    @Test
    void rechazaElRegistroDeUnEmailDuplicado() {
        registrar.ejecutar("ana@correo.com", "Secreta123");

        assertThrows(ConflictoException.class, () -> registrar.ejecutar("ana@correo.com", "Otra456"));
        assertEquals(1, repositorio.count());
    }

    @Test
    void loginCorrectoEmiteUnTokenConIdYRol() {
        var usuario = registrar.ejecutar("ana@correo.com", "Secreta123");

        var datos = tokens.leer(iniciarSesion.ejecutar("ana@correo.com", "Secreta123"));

        assertEquals(usuario.id(), datos.id());
        assertEquals("USUARIO", datos.rol());
    }

    @Test
    void loginDeUnAdministradorLlevaSuRol() {
        registrar.ejecutar("root@correo.com", "Secreta123");
        repositorio.findByEmail("root@correo.com").orElseThrow().setRol(Rol.ADMIN);

        assertEquals("ADMIN", tokens.leer(iniciarSesion.ejecutar("root@correo.com", "Secreta123")).rol());
    }

    @Test
    void contrasenaIncorrectaYEmailInexistenteDanElMismoError() {
        registrar.ejecutar("ana@correo.com", "Secreta123");

        var contrasenaMala = assertThrows(BadCredentialsException.class,
                () -> iniciarSesion.ejecutar("ana@correo.com", "equivocada"));
        var emailMalo = assertThrows(BadCredentialsException.class,
                () -> iniciarSesion.ejecutar("nadie@correo.com", "Secreta123"));

        assertEquals(contrasenaMala.getMessage(), emailMalo.getMessage());
    }

    @Test
    void creaElAdministradorInicialConRolAdminYContrasenaCifrada() {
        crearAdministrador.ejecutar("root@correo.com", "Secreta123");

        var admin = repositorio.findByEmail("root@correo.com").orElseThrow();
        assertEquals(Rol.ADMIN, admin.getRol());
        assertTrue(codificador.matches("Secreta123", admin.getContrasena()));
    }

    @Test
    void crearElAdministradorDosVecesNoDuplicaNiCambiaLaContrasena() {
        crearAdministrador.ejecutar("root@correo.com", "Secreta123");
        crearAdministrador.ejecutar("root@correo.com", "Distinta456");

        assertEquals(1, repositorio.count());
        var admin = repositorio.findByEmail("root@correo.com").orElseThrow();
        assertTrue(codificador.matches("Secreta123", admin.getContrasena()));
    }

    @Test
    void noPromueveAUnUsuarioYaRegistradoConEseEmail() {
        registrar.ejecutar("ana@correo.com", "Secreta123");

        crearAdministrador.ejecutar("ana@correo.com", "Distinta456");

        assertEquals(Rol.USUARIO, repositorio.findByEmail("ana@correo.com").orElseThrow().getRol());
    }
}
