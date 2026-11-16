package shop.authentication.internal.domain.service;

import shop.authentication.internal.domain.dto.LoginDto;
import shop.authentication.internal.domain.dto.RegisterUserDto;
import shop.authentication.internal.domain.dto.UserDto;
import shop.authentication.internal.domain.exception.EmailAlreadyRegisteredException;
import shop.authentication.internal.domain.exception.IncompleteAdminConfigurationException;
import shop.authentication.internal.domain.exception.LoginFailedException;

/** Authentication rules: registration, credential check and the initial administrator. */
public interface AuthService {

    /**
     * Registers a user with the USUARIO role and the password hashed. The returned user never carries the hash.
     *
     * @throws EmailAlreadyRegisteredException if the email is taken
     */
    UserDto register(RegisterUserDto request);

    /**
     * Checks the credentials and returns the user's token.
     *
     * @throws LoginFailedException for unknown email and wrong password alike
     */
    String login(LoginDto credentials);

    /**
     * Startup configuration of the first admin: with neither value it does nothing; with both it creates the admin
     * if the email is free (never modifies an existing user).
     *
     * @throws IncompleteAdminConfigurationException if only one of the two is configured
     */
    void ensureInitialAdmin(String email, String rawPassword);
}
