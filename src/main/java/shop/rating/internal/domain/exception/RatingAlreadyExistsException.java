package shop.rating.internal.domain.exception;

import shop.shared.error.ConflictException;

/** The user already has a rating of that movie: two simultaneous first ratings collided (409). */
public class RatingAlreadyExistsException extends ConflictException {

    public RatingAlreadyExistsException(Long movieId) {
        super("Ya existe una valoración tuya de la película " + movieId);
    }
}
