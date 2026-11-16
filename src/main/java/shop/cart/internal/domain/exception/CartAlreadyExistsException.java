package shop.cart.internal.domain.exception;

import shop.shared.error.ConflictException;

/** The user already has a cart: the UNIQUE constraint on the user lost a race. The service recovers from it. */
public class CartAlreadyExistsException extends ConflictException {

    public CartAlreadyExistsException(Long userId) {
        super("El usuario " + userId + " ya tiene carrito");
    }
}
