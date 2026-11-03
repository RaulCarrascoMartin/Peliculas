package shop.shared.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokensTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes-of-length";

    private final JwtTokens tokens = new JwtTokens(SECRET, Duration.ofHours(1));

    @Test
    void tokenCarriesIdAndRole() {
        var data = tokens.parseToken(tokens.issueToken(42L, "ADMIN"));

        assertEquals(42L, data.id());
        assertEquals("ADMIN", data.role());
    }

    @Test
    void rejectsExpiredToken() {
        var expired = new JwtTokens(SECRET, Duration.ofSeconds(-10));

        assertThrows(JwtException.class, () -> tokens.parseToken(expired.issueToken(1L, "USUARIO")));
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        var others = new JwtTokens("another-different-secret-with-32-bytes-or-more!!", Duration.ofHours(1));

        assertThrows(JwtException.class, () -> tokens.parseToken(others.issueToken(1L, "USUARIO")));
    }

    @Test
    void rejectsTamperedToken() {
        String token = tokens.issueToken(1L, "USUARIO");
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThrows(JwtException.class, () -> tokens.parseToken(tampered));
    }
}
