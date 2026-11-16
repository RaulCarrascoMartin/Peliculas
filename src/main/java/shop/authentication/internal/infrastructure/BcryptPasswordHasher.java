package shop.authentication.internal.infrastructure;

import org.springframework.security.crypto.password.PasswordEncoder;
import shop.authentication.internal.domain.contract.PasswordHasher;

/** {@link PasswordHasher} over the application's Spring Security {@link PasswordEncoder} (BCrypt). */
public class BcryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder;

    public BcryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
