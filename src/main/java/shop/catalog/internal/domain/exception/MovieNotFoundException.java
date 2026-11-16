package shop.catalog.internal.domain.exception;

import shop.shared.error.ResourceNotFoundException;

/** The movie does not exist (404). */
public class MovieNotFoundException extends ResourceNotFoundException {

    private final Long movieId;

    public MovieNotFoundException(Long movieId) {
        super("Película " + movieId + " no encontrada");
        this.movieId = movieId;
    }

    public Long movieId() {
        return movieId;
    }
}
