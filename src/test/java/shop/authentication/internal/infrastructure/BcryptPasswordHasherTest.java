package shop.authentication.internal.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BcryptPasswordHasherTest {

    private final BcryptPasswordHasher hasher = new BcryptPasswordHasher(new BCryptPasswordEncoder());

    @Test
    void hashIsBcryptAndNeverTheRawPassword() {
        String hash = hasher.hash("Secreta123");

        assertTrue(hash.startsWith("$2"));
        assertNotEquals("Secreta123", hash);
    }

    @Test
    void matchesOnlyThePasswordThatWasHashed() {
        String hash = hasher.hash("Secreta123");

        assertTrue(hasher.matches("Secreta123", hash));
        assertFalse(hasher.matches("otra", hash));
    }

    @Test
    void samePasswordHashedTwiceGivesDifferentHashesThatBothMatch() {
        String first = hasher.hash("Secreta123");
        String second = hasher.hash("Secreta123");

        assertNotEquals(first, second);
        assertTrue(hasher.matches("Secreta123", first));
        assertTrue(hasher.matches("Secreta123", second));
    }
}
