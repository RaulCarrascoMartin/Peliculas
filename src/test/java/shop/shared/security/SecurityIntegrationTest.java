package shop.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityIntegrationTest.ProtectedController.class)
class SecurityIntegrationTest {

    @RestController
    static class ProtectedController {
        private final CurrentUser currentUser;

        ProtectedController(CurrentUser currentUser) {
            this.currentUser = currentUser;
        }

        @GetMapping("/api/protegido")
        String guarded() {
            return "user " + currentUser.id();
        }

        @GetMapping("/api/administracion")
        @PreAuthorize("hasRole('ADMIN')")
        String administration() {
            return "admin " + currentUser.id();
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @Value("${peliculas.jwt.secreto}")
    private String secret;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void protectedRouteWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/protegido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void validTokenGrantsAccessAndIdentifiesUser() throws Exception {
        mvc.perform(get("/api/protegido").header("Authorization", bearer(tokens.issueToken(7L, "USUARIO"))))
                .andExpect(status().isOk())
                .andExpect(content().string("user 7"));
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        String expired = new JwtTokens(secret, Duration.ofSeconds(-10)).issueToken(7L, "USUARIO");

        mvc.perform(get("/api/protegido").header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithTamperedSignatureReturns401() throws Exception {
        String token = tokens.issueToken(7L, "USUARIO");
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        mvc.perform(get("/api/protegido").header("Authorization", bearer(tampered)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherSecretReturns401() throws Exception {
        String others = new JwtTokens("another-different-secret-with-32-bytes-or-more!!", Duration.ofHours(1))
                .issueToken(7L, "ADMIN");

        mvc.perform(get("/api/protegido").header("Authorization", bearer(others)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void headerWithoutBearerSchemeReturns401() throws Exception {
        mvc.perform(get("/api/protegido").header("Authorization", tokens.issueToken(7L, "USUARIO")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void insufficientRoleReturns403() throws Exception {
        mvc.perform(get("/api/administracion").header("Authorization", bearer(tokens.issueToken(7L, "USUARIO"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));
    }

    @Test
    void adminCanAccessAdminRoute() throws Exception {
        mvc.perform(get("/api/administracion").header("Authorization", bearer(tokens.issueToken(1L, "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().string("admin 1"));
    }

    @Test
    void registerAndLoginArePublicAndObtainedTokenWorks() throws Exception {
        String credentials = "{\"email\":\"ana@correo.com\",\"contrasena\":\"Secreta123\"}";
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());

        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = response.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mvc.perform(get("/api/protegido").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void nonJwtTokenReturns401() throws Exception {
        mvc.perform(get("/api/protegido").header("Authorization", bearer("this-is-not-a-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void duplicateRegisterReturns409WithRealSecurity() throws Exception {
        String credentials = "{\"email\":\"duplicado@correo.com\",\"contrasena\":\"Secreta123\"}";
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409));
    }

    @Test
    void badLoginReturns401WithoutSayingWhichFieldFailed() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@correo.com\",\"contrasena\":\"Secreta123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
    }

    /** The seven cart and purchase routes (requirement 7.1). */
    private record Ruta(HttpMethod method, String url) {
    }

    private static final List<Ruta> CART_AND_PURCHASE_ROUTES = List.of(
            new Ruta(HttpMethod.GET, "/api/carrito"),
            new Ruta(HttpMethod.POST, "/api/carrito/lineas"),
            new Ruta(HttpMethod.DELETE, "/api/carrito/lineas/1"),
            new Ruta(HttpMethod.DELETE, "/api/carrito"),
            new Ruta(HttpMethod.POST, "/api/compras"),
            new Ruta(HttpMethod.GET, "/api/compras"),
            new Ruta(HttpMethod.GET, "/api/compras/1"));

    private void assert401(Ruta ruta, String header) throws Exception {
        var request = request(ruta.method(), ruta.url())
                .contentType(MediaType.APPLICATION_JSON).content("{\"peliculaId\":1}");
        if (header != null) {
            request.header("Authorization", header);
        }
        mvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void allSevenCartAndPurchaseRoutesReturn401WithoutToken() throws Exception {
        for (Ruta ruta : CART_AND_PURCHASE_ROUTES) {
            assert401(ruta, null);
        }
    }

    @Test
    void allSevenCartAndPurchaseRoutesReturn401WithExpiredToken() throws Exception {
        String expired = new JwtTokens(secret, Duration.ofSeconds(-10)).issueToken(7L, "USUARIO");
        for (Ruta ruta : CART_AND_PURCHASE_ROUTES) {
            assert401(ruta, bearer(expired));
        }
    }

    @Test
    void allSevenCartAndPurchaseRoutesReturn401WithInvalidToken() throws Exception {
        String others = new JwtTokens("another-different-secret-with-32-bytes-or-more!!", Duration.ofHours(1))
                .issueToken(7L, "USUARIO");
        for (Ruta ruta : CART_AND_PURCHASE_ROUTES) {
            assert401(ruta, bearer("this-is-not-a-jwt"));
            assert401(ruta, bearer(others));
        }
    }

    @Test
    void publishedContractsDeclareNoUserIdInPathsOrBodies() throws Exception {
        for (String contract : List.of("/contratos/carrito-api.yml", "/contratos/compras-api.yml")) {
            String contractText = mvc.perform(get(contract)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertFalse(contractText.contains("usuarioId"), contract + " declares usuarioId");
            assertFalse(contractText.contains("usuario_id"), contract + " declares usuario_id");
            assertFalse(contractText.contains("{usuario"), contract + " declares a user path parameter");
        }
    }
}
