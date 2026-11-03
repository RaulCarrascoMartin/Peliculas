package shop.authentication.internal.usecase;

import shop.authentication.internal.domain.Role;
import shop.authentication.internal.domain.User;
import shop.authentication.internal.domain.UserRepository;
import shop.shared.error.ConflictException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterUser {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public RegisterUser(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public UserView execute(String email, String password) {
        if (users.existsByEmail(email)) {
            throw new ConflictException("El email ya está registrado");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setRole(Role.USUARIO);
        User saved = users.save(user);
        return new UserView(saved.getId(), saved.getEmail(), saved.getRole().name());
    }
}
