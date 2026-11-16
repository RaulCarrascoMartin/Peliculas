package shop.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and validates the API's JWTs. The token carries the user id in {@code sub}
 * and the role in the {@code rol} claim. It works with primitive types so as not to depend on any module.
 */
@Component
public class JwtTokens {

    private static final String ROLE_CLAIM = "rol";

    private final SecretKey key;
    private final Duration expiration;

    public JwtTokens(@Value("${peliculas.jwt.secreto}") String secret,
                     @Value("${peliculas.jwt.caducidad}") Duration expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String issueToken(Long id, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(id))
                .claim(ROLE_CLAIM, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /** Validates signature and expiry; throws {@link io.jsonwebtoken.JwtException} if the token is not valid. */
    public TokenData parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new TokenData(Long.valueOf(claims.getSubject()), claims.get(ROLE_CLAIM, String.class));
    }
}
