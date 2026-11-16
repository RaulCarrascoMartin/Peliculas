package shop.authentication.api.http.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import shop.authentication.api.http.mapper.AuthWebMapperImpl;
import shop.authentication.internal.domain.dto.LoginDto;
import shop.authentication.internal.domain.dto.RegisterUserDto;
import shop.authentication.internal.domain.dto.UserDto;
import shop.authentication.internal.domain.exception.EmailAlreadyRegisteredException;
import shop.authentication.internal.domain.exception.LoginFailedException;
import shop.authentication.internal.domain.model.Role;
import shop.authentication.internal.domain.service.AuthService;
import shop.shared.security.JwtTokens;
import shop.shared.security.SecurityConfig;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP contract of authentication with the service mocked: codes, bodies, format validation and errors. */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtTokens.class, AuthWebMapperImpl.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;

    private ResultActions send(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"contrasena\":\"" + password + "\"}";
    }

    @Test
    void validRegisterReturns201WithoutBodyAndWithoutToken() throws Exception {
        when(authService.register(new RegisterUserDto("ana@correo.com", "Secreta123")))
                .thenReturn(new UserDto(1L, "ana@correo.com", null, Role.USUARIO));

        send("/api/auth/registro", credentials("ana@correo.com", "Secreta123"))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        verify(authService).register(new RegisterUserDto("ana@correo.com", "Secreta123"));
    }

    @Test
    void registerAcceptsPasswordsOfExactly8And72Characters() throws Exception {
        send("/api/auth/registro", credentials("ana@correo.com", "a".repeat(8))).andExpect(status().isCreated());
        send("/api/auth/registro", credentials("ana@correo.com", "a".repeat(72))).andExpect(status().isCreated());
    }

    @Test
    void registerWithInvalidEmailOrPasswordOutOfRangeReturns400AndNeverReachesTheService() throws Exception {
        send("/api/auth/registro", credentials("not-an-email", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasItem(startsWith("email: "))))
                .andExpect(jsonPath("$.errors", hasItem(startsWith("contrasena: "))));
        send("/api/auth/registro", credentials("ana@correo.com", "a".repeat(7)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(startsWith("contrasena: "))));
        send("/api/auth/registro", credentials("ana@correo.com", "a".repeat(73)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(startsWith("contrasena: "))));
        send("/api/auth/registro", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(startsWith("email: "))))
                .andExpect(jsonPath("$.errors", hasItem(startsWith("contrasena: "))));

        verifyNoInteractions(authService);
    }

    @Test
    void registerWithDuplicateEmailReturns409AsProblemDetail() throws Exception {
        when(authService.register(any())).thenThrow(new EmailAlreadyRegisteredException());

        send("/api/auth/registro", credentials("ana@correo.com", "Secreta123"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("El email ya está registrado"));
    }

    @Test
    void successfulLoginReturns200WithOnlyTheToken() throws Exception {
        when(authService.login(new LoginDto("ana@correo.com", "Secreta123"))).thenReturn("the.jwt.token");

        send("/api/auth/login", credentials("ana@correo.com", "Secreta123"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"token\":\"the.jwt.token\"}", true));
    }

    @Test
    void loginWithBadCredentialsReturns401AsProblemDetail() throws Exception {
        when(authService.login(any())).thenThrow(new LoginFailedException());

        send("/api/auth/login", credentials("ana@correo.com", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Credenciales incorrectas"));
    }

    @Test
    void loginWithoutDataReturns400AndNeverReachesTheService() throws Exception {
        send("/api/auth/login", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(startsWith("email: "))))
                .andExpect(jsonPath("$.errors", hasItem(startsWith("contrasena: "))));

        verifyNoInteractions(authService);
    }

    @Test
    void bothRoutesArePublicAndTheOthersOfTheModuleDoNotExist() throws Exception {
        when(authService.login(any())).thenReturn("the.jwt.token");

        // no Authorization header anywhere in this class: register and login are reachable without identity
        send("/api/auth/login", credentials("ana@correo.com", "Secreta123")).andExpect(status().isOk());
        send("/api/auth/otra", "{}").andExpect(status().isUnauthorized());
    }
}
