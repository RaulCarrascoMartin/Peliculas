package shop.authentication.internal.usecase;

import shop.authentication.internal.domain.Role;
import shop.authentication.internal.domain.User;
import shop.authentication.internal.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CreateAdminIfMissing {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public CreateAdminIfMissing(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    /** Creates an admin if no user with that email exists yet; never modifies an existing one. */
    public void execute(String email, String password) {
        if (users.existsByEmail(email)) {
            return;
        }
        User admin = new User();
        admin.setEmail(email);
        admin.setPassword(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        users.save(admin);
    }
}
