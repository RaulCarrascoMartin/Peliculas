package shop.authentication.internal.domain.service;

import shop.authentication.internal.domain.contract.PasswordHasher;
import shop.authentication.internal.domain.contract.TokenIssuer;
import shop.authentication.internal.domain.exception.EmailAlreadyRegisteredException;
import shop.authentication.internal.domain.exception.LoginFailedException;
import shop.authentication.internal.domain.model.Role;
import shop.authentication.internal.domain.model.User;
import shop.authentication.internal.domain.repository.UserRepository;

public class AuthServiceImpl implements AuthService {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final TokenIssuer tokens;

    public AuthServiceImpl(UserRepository users, PasswordHasher hasher, TokenIssuer tokens) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
    }

    @Override
    public User register(String email, String rawPassword) {
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        return create(email, rawPassword, Role.USUARIO);
    }

    @Override
    public String login(String email, String rawPassword) {
        return users.findByEmail(email)
                .filter(u -> hasher.matches(rawPassword, u.getPassword()))
                .map(u -> tokens.issue(u.getId(), u.getRole().name()))
                .orElseThrow(LoginFailedException::new);
    }

    @Override
    public void ensureInitialAdmin(String email, String rawPassword) {
        boolean noEmail = email == null || email.isBlank();
        boolean noPassword = rawPassword == null || rawPassword.isBlank();
        if (noEmail && noPassword) {
            return;
        }
        if (noEmail || noPassword) {
            throw new IllegalStateException(
                    "peliculas.admin.email and peliculas.admin.contrasena must be configured together");
        }
        if (!users.existsByEmail(email)) {
            create(email, rawPassword, Role.ADMIN);
        }
    }

    private User create(String email, String rawPassword, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(hasher.hash(rawPassword));
        user.setRole(role);
        return users.save(user);
    }
}
