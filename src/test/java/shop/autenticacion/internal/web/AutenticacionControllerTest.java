package shop.autenticacion.internal.web;

import shop.autenticacion.internal.usecase.IniciarSesion;
import shop.autenticacion.internal.usecase.RegistrarUsuario;
import shop.autenticacion.internal.usecase.UsuarioRepositoryFalso;
import shop.compartido.error.GlobalExceptionHandler;
import shop.compartido.seguridad.JwtTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutenticacionControllerTest {

    private final JwtTokens tokens = new JwtTokens(
            "secreto-de-pruebas-con-al-menos-32-bytes-de-longitud", Duration.ofHours(1));

    private UsuarioRepositoryFalso repositorio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        repositorio = new UsuarioRepositoryFalso();
        var codificador = new BCryptPasswordEncoder();
        mvc = MockMvcBuilders.standaloneSetup(new AutenticacionController(
                new RegistrarUsuario(repositorio, codificador), new IniciarSesion(repositorio, codificador, tokens)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private org.springframework.test.web.servlet.ResultActions enviar(String ruta, String json) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String credenciales(String email, String contrasena) {
        return "{\"email\":\"" + email + "\",\"contrasena\":\"" + contrasena + "\"}";
    }

    @Test
    void registroValidoDevuelve201YGuardaElUsuario() throws Exception {
        enviar("/api/auth/registro", credenciales("ana@correo.com", "Secreta123"))
                .andExpect(status().isCreated());

        assertEquals(1, repositorio.count());
    }

    @Test
    void registroConEmailInvalidoODemasiadoCortaDevuelve400() throws Exception {
        enviar("/api/auth/registro", credenciales("no-es-un-email", "corta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").isNotEmpty())
                .andExpect(jsonPath("$.errores.contrasena").isNotEmpty());

        assertEquals(0, repositorio.count());
    }

    @Test
    void registroConEmailDuplicadoDevuelve409() throws Exception {
        enviar("/api/auth/registro", credenciales("ana@correo.com", "Secreta123"));

        enviar("/api/auth/registro", credenciales("ana@correo.com", "Secreta123"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409));
    }

    @Test
    void loginCorrectoDevuelveElToken() throws Exception {
        enviar("/api/auth/registro", credenciales("ana@correo.com", "Secreta123"));

        enviar("/api/auth/login", credenciales("ana@correo.com", "Secreta123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginConCredencialesIncorrectasDevuelve401() throws Exception {
        enviar("/api/auth/registro", credenciales("ana@correo.com", "Secreta123"));

        enviar("/api/auth/login", credenciales("ana@correo.com", "equivocada"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
    }

    @Test
    void loginSinDatosDevuelve400() throws Exception {
        enviar("/api/auth/login", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").isNotEmpty())
                .andExpect(jsonPath("$.errores.contrasena").isNotEmpty());
    }
}
