package shop.authentication.internal.usecase;

import shop.authentication.internal.domain.UserRepository;
import shop.shared.security.JwtTokens;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class Login {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtTokens tokens;

    public Login(UserRepository users, PasswordEncoder encoder, JwtTokens tokens) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    private static final String BAD_CREDENTIALS_MESSAGE = "Credenciales incorrectas";

    /** Returns the user's JWT; the same error is used for unknown email and wrong password. */
    public String execute(String email, String password) {
        return users.findByEmail(email)
                .filter(u -> encoder.matches(password, u.getPassword()))
                .map(u -> tokens.issueToken(u.getId(), u.getRole().name()))
                .orElseThrow(() -> new BadCredentialsException(BAD_CREDENTIALS_MESSAGE));
    }
}
