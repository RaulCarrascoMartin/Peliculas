package shop.cart.internal.domain.exception;

import shop.shared.error.ConflictException;

/** The movie is already a line of the cart (409). */
public class MovieAlreadyInCartException extends ConflictException {

    public MovieAlreadyInCartException() {
        super("La película ya está en el carrito");
    }
}
