package shop.authentication.internal.domain.exception;

import shop.shared.error.ConflictException;

/** A user with that email already exists (409). */
public class EmailAlreadyRegisteredException extends ConflictException {

    public EmailAlreadyRegisteredException() {
        super("El email ya está registrado");
    }
}
