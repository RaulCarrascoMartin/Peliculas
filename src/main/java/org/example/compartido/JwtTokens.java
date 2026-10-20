package org.example.compartido;

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
 * Emite y valida los JWT de la API. El token lleva el id del usuario en {@code sub}
 * y su rol en el claim {@code rol}. Trabaja con tipos primitivos para no depender de ningún módulo.
 */
@Component
public class JwtTokens {

    private static final String CLAIM_ROL = "rol";

    private final SecretKey clave;
    private final Duration caducidad;

    public JwtTokens(@Value("${peliculas.jwt.secreto}") String secreto,
                     @Value("${peliculas.jwt.caducidad}") Duration caducidad) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.caducidad = caducidad;
    }

    public String emitir(Long id, String rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(id))
                .claim(CLAIM_ROL, rol)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(caducidad)))
                .signWith(clave)
                .compact();
    }

    /** Valida firma y caducidad; lanza {@link io.jsonwebtoken.JwtException} si el token no es válido. */
    public DatosToken leer(String token) {
        Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
        return new DatosToken(Long.valueOf(claims.getSubject()), claims.get(CLAIM_ROL, String.class));
    }
}
