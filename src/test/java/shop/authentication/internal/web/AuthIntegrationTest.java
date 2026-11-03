package shop.authentication.internal.web;

import com.jayway.jsonpath.JsonPath;
import shop.authentication.internal.domain.Role;
import shop.authentication.internal.domain.UserRepository;
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
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(credentials()))
                .andExpect(status().isCreated());

        var user = users.findByEmail(EMAIL).orElseThrow();
        assertEquals(Role.USUARIO, user.getRole());
        assertNotEquals(PASSWORD, user.getPassword());
        assertTrue(user.getPassword().startsWith("$2"), "must be a BCrypt hash");
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
        assertEquals(users.findByEmail(EMAIL).orElseThrow().getId(), data.id());
        assertEquals("USUARIO", data.role());
    }
}
