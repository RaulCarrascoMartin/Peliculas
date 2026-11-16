package shop.cart.internal.domain.exception;

import shop.shared.error.ResourceNotFoundException;

/** The movie is not a line of the user's cart (404). */
public class MovieNotInCartException extends ResourceNotFoundException {

    public MovieNotInCartException(Long movieId) {
        super("La película " + movieId + " no está en el carrito");
    }
}
