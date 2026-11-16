package shop.authentication.internal.domain.exception;

import shop.shared.error.InvalidCredentialsException;

/** Unknown email or wrong password: the same error for both, so it does not reveal which emails exist (401). */
public class LoginFailedException extends InvalidCredentialsException {

    public LoginFailedException() {
        super("Credenciales incorrectas");
    }
}
