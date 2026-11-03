package shop.cart.internal.domain.exception;

/** The user already has a cart: the UNIQUE constraint on the user lost a race. */
public class CartAlreadyExistsException extends RuntimeException {

    public CartAlreadyExistsException(Long userId) {
        super("El usuario " + userId + " ya tiene carrito");
    }
}
