package shop.rating.internal.domain.exception;

import shop.rating.internal.domain.model.MarkType;
import shop.shared.error.ConflictException;

/** The user already has the movie in that list: two simultaneous marks collided (409). */
public class MovieAlreadyMarkedException extends ConflictException {

    public MovieAlreadyMarkedException(Long movieId, MarkType type) {
        super("La película " + movieId + " ya está marcada como " + type);
    }
}
