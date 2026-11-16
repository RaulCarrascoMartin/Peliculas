package shop.rating.internal.domain.exception;

import shop.shared.error.ResourceNotFoundException;

/** The movie to rate or mark does not exist in the catalog (404). */
public class MovieNotFoundException extends ResourceNotFoundException {

    public MovieNotFoundException(Long movieId) {
        super("Película " + movieId + " no encontrada");
    }
}
