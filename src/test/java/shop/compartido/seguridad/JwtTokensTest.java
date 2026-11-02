package shop.compartido.seguridad;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokensTest {

    private static final String SECRETO = "secreto-de-pruebas-con-al-menos-32-bytes-de-longitud";

    private final JwtTokens tokens = new JwtTokens(SECRETO, Duration.ofHours(1));

    @Test
    void elTokenLlevaElIdYElRol() {
        var datos = tokens.leer(tokens.emitir(42L, "ADMIN"));

        assertEquals(42L, datos.id());
        assertEquals("ADMIN", datos.rol());
    }

    @Test
    void rechazaUnTokenCaducado() {
        var caducado = new JwtTokens(SECRETO, Duration.ofSeconds(-10));

        assertThrows(JwtException.class, () -> tokens.leer(caducado.emitir(1L, "USUARIO")));
    }

    @Test
    void rechazaUnTokenFirmadoConOtroSecreto() {
        var ajeno = new JwtTokens("otro-secreto-distinto-con-32-bytes-o-mas!!", Duration.ofHours(1));

        assertThrows(JwtException.class, () -> tokens.leer(ajeno.emitir(1L, "USUARIO")));
    }

    @Test
    void rechazaUnTokenAlterado() {
        String token = tokens.emitir(1L, "USUARIO");
        String alterado = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThrows(JwtException.class, () -> tokens.leer(alterado));
    }
}
