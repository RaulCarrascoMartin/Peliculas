package shop.authentication.internal.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import shop.authentication.internal.domain.dto.LoginDto;
import shop.authentication.internal.domain.service.AuthService;
import shop.shared.security.JwtTokens;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "peliculas.admin.email=admin.inicial@correo.com",
        "peliculas.admin.contrasena=ClaveInicial123"
})
class InitialAdminIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokens tokens;

    @Test
    void onStartupAdminExistsAndCanLogIn() {
        String token = authService.login(new LoginDto("admin.inicial@correo.com", "ClaveInicial123"));

        assertEquals("ADMIN", tokens.parseToken(token).role());
    }
}
