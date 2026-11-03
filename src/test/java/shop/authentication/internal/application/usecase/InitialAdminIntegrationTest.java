package shop.authentication.internal.application.usecase;

import shop.shared.security.JwtTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "peliculas.admin.email=admin.inicial@correo.com",
        "peliculas.admin.contrasena=ClaveInicial123"
})
class InitialAdminIntegrationTest {

    @Autowired
    private Login login;

    @Autowired
    private JwtTokens tokens;

    @Test
    void onStartupAdminExistsAndCanLogIn() {
        String token = login.execute("admin.inicial@correo.com", "ClaveInicial123");

        assertEquals("ADMIN", tokens.parseToken(token).role());
    }
}
