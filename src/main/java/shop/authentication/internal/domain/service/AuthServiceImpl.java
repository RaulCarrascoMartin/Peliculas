package shop.authentication.internal.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.authentication.internal.domain.contract.PasswordHasher;
import shop.authentication.internal.domain.contract.TokenProvider;
import shop.authentication.internal.domain.dto.LoginDto;
import shop.authentication.internal.domain.dto.RegisterUserDto;
import shop.authentication.internal.domain.dto.UserDto;
import shop.authentication.internal.domain.exception.EmailAlreadyRegisteredException;
import shop.authentication.internal.domain.exception.IncompleteAdminConfigurationException;
import shop.authentication.internal.domain.exception.LoginFailedException;
import shop.authentication.internal.domain.mapper.UserMapper;
import shop.authentication.internal.domain.model.Role;
import shop.authentication.internal.domain.model.User;
import shop.authentication.internal.domain.repository.UserRepository;

/**
 * The password hash stays between this service and the repository: every {@link UserDto} that leaves towards the
 * controller or the token provider is built with {@link UserMapper#toPublicDto}.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final TokenProvider tokens;
    private final UserMapper mapper;

    public AuthServiceImpl(UserRepository users, PasswordHasher hasher, TokenProvider tokens, UserMapper mapper) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public UserDto register(RegisterUserDto request) {
        if (users.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException();
        }
        return mapper.toPublicDto(create(request.email(), request.password(), Role.USUARIO));
    }

    @Override
    @Transactional(readOnly = true)
    public String login(LoginDto credentials) {
        return users.findByEmail(credentials.email())
                .map(mapper::toModel)
                .filter(user -> hasher.matches(credentials.password(), user.getPassword()))
                .map(user -> tokens.generate(mapper.toPublicDto(user)))
                .orElseThrow(LoginFailedException::new);
    }

    @Override
    @Transactional
    public void ensureInitialAdmin(String email, String rawPassword) {
        boolean noEmail = email == null || email.isBlank();
        boolean noPassword = rawPassword == null || rawPassword.isBlank();
        if (noEmail && noPassword) {
            return;
        }
        if (noEmail || noPassword) {
            throw new IncompleteAdminConfigurationException();
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
        return mapper.toModel(users.save(mapper.toDto(user)));
    }
}
