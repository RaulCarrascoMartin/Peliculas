package shop.authentication.api.http.controller;

import shop.authentication.internal.domain.service.AuthServiceImpl;
import shop.authentication.internal.infrastructure.BcryptPasswordHasher;
import shop.authentication.internal.infrastructure.JwtTokenIssuer;
import shop.authentication.internal.application.usecase.Login;
import shop.authentication.internal.application.usecase.RegisterUser;
import shop.authentication.internal.domain.fake.UserRepositoryFake;
import shop.shared.error.GlobalExceptionHandler;
import shop.shared.security.JwtTokens;
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

class AuthControllerTest {

    private final JwtTokens tokens = new JwtTokens(
            "test-secret-with-at-least-32-bytes-of-length", Duration.ofHours(1));

    private UserRepositoryFake repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository = new UserRepositoryFake();
        var encoder = new BcryptPasswordHasher(new BCryptPasswordEncoder());
        var service = new AuthServiceImpl(repository, encoder, new JwtTokenIssuer(tokens));
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(
                new RegisterUser(service), new Login(service)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private org.springframework.test.web.servlet.ResultActions send(String ruta, String json) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"contrasena\":\"" + password + "\"}";
    }

    @Test
    void validRegisterReturns201AndSavesUser() throws Exception {
        send("/api/auth/registro", credentials("ana@correo.com", "Secreta123"))
                .andExpect(status().isCreated());

        assertEquals(1, repository.count());
    }

    @Test
    void registerWithInvalidEmailOrTooShortPasswordReturns400() throws Exception {
        send("/api/auth/registro", credentials("not-an-email", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").isNotEmpty())
                .andExpect(jsonPath("$.errores.contrasena").isNotEmpty());

        assertEquals(0, repository.count());
    }

    @Test
    void registerWithDuplicateEmailReturns409() throws Exception {
        send("/api/auth/registro", credentials("ana@correo.com", "Secreta123"));

        send("/api/auth/registro", credentials("ana@correo.com", "Secreta123"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409));
    }

    @Test
    void successfulLoginReturnsToken() throws Exception {
        send("/api/auth/registro", credentials("ana@correo.com", "Secreta123"));

        send("/api/auth/login", credentials("ana@correo.com", "Secreta123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginWithBadCredentialsReturns401() throws Exception {
        send("/api/auth/registro", credentials("ana@correo.com", "Secreta123"));

        send("/api/auth/login", credentials("ana@correo.com", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
    }

    @Test
    void loginWithoutDataReturns400() throws Exception {
        send("/api/auth/login", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").isNotEmpty())
                .andExpect(jsonPath("$.errores.contrasena").isNotEmpty());
    }
}
