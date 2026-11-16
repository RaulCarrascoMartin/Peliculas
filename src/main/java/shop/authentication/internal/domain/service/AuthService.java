package shop.authentication.internal.domain.service;

import shop.authentication.internal.domain.exception.EmailAlreadyRegisteredException;
import shop.authentication.internal.domain.exception.LoginFailedException;
import shop.authentication.internal.domain.model.User;

/** Authentication rules: registration, credential check and the initial administrator. */
public interface AuthService {

    /**
     * Registers a user with the USUARIO role and the password hashed.
     *
     * @throws EmailAlreadyRegisteredException if the email is taken
     */
    User register(String email, String rawPassword);

    /**
     * Checks the credentials and returns the user's token.
     *
     * @throws LoginFailedException for unknown email and wrong password alike
     */
    String login(String email, String rawPassword);

    /**
     * Startup configuration of the first admin: with neither value it does nothing; with both it creates the admin
     * if the email is free (never modifies an existing user).
     *
     * @throws IllegalStateException if only one of the two is configured
     */
    void ensureInitialAdmin(String email, String rawPassword);
}
