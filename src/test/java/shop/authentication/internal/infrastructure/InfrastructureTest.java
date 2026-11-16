package shop.authentication.internal.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import shop.shared.security.JwtTokens;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The two technical pieces of the module: BCrypt hashing and JWT issuing. */
class InfrastructureTest {

    @Test
    void hasherHashesAndMatches() {
        var hasher = new BcryptPasswordHasher(new BCryptPasswordEncoder());

        String hash = hasher.hash("Secreta123");

        assertTrue(hash.startsWith("$2"));
        assertTrue(hasher.matches("Secreta123", hash));
        assertFalse(hasher.matches("otra", hash));
    }

    @Test
    void issuerPutsUserIdAndRoleInTheToken() {
        var jwt = new JwtTokens("secreto-de-pruebas-con-al-menos-32-bytes-de-longitud", Duration.ofHours(1));

        var data = jwt.parseToken(new JwtTokenIssuer(jwt).issue(5L, "ADMIN"));

        assertEquals(5L, data.id());
        assertEquals("ADMIN", data.role());
    }
}
