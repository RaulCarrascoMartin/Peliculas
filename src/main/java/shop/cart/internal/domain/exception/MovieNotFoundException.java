package shop.cart.internal.domain.exception;

import shop.shared.error.ResourceNotFoundException;

/** The movie is not in the catalog (404). */
public class MovieNotFoundException extends ResourceNotFoundException {

    public MovieNotFoundException(Long movieId) {
        super("La película " + movieId + " no existe");
    }
}
