package shop.authentication.api.http.controller;

import com.jayway.jsonpath.JsonPath;
import shop.authentication.internal.domain.model.Role;
import shop.authentication.internal.domain.repository.UserRepository;
import shop.shared.security.JwtTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Requirement 0 with the whole application: account with user role, encoded password and token with id and role. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    private static final String EMAIL = "integracion.auth@correo.com";
    private static final String PASSWORD = "Secreta123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtTokens tokens;

    private static String credentials() {
        return "{\"email\":\"" + EMAIL + "\",\"contrasena\":\"" + PASSWORD + "\"}";
    }

    @Test
    void registerCreatesAccountWithUserRoleAndEncodedPassword() throws Exception {
        if (users.findByEmail(EMAIL).isEmpty()) {
            mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(credentials()))
                    .andExpect(status().isCreated());
        }

        var user = users.findByEmail(EMAIL).orElseThrow();
        assertEquals(Role.USUARIO, user.role());
        assertNotEquals(PASSWORD, user.passwordHash());
        assertTrue(user.passwordHash().startsWith("$2"), "must be a BCrypt hash");
    }

    @Test
    void loginReturnsTokenWithUserIdAndRole() throws Exception {
        if (users.findByEmail(EMAIL).isEmpty()) {
            mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(credentials()))
                    .andExpect(status().isCreated());
        }

        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        var data = tokens.parseToken(JsonPath.read(body, "$.token"));
        assertEquals(users.findByEmail(EMAIL).orElseThrow().id(), data.id());
        assertEquals("USUARIO", data.role());
    }

    @Test
    void secondRegistrationOfTheSameEmailGives409AndWrongCredentialsGive401() throws Exception {
        String other = "{\"email\":\"duplicado.auth@correo.com\",\"contrasena\":\"" + PASSWORD + "\"}";
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(other))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(other))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El email ya está registrado"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"duplicado.auth@correo.com\",\"contrasena\":\"otra-clave\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciales incorrectas"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie.auth@correo.com\",\"contrasena\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciales incorrectas"));
    }
}
