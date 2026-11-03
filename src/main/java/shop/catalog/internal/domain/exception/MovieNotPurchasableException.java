package shop.catalog.internal.domain.exception;

import shop.shared.error.ConflictException;

/** The movie exists but has no price, so its stock cannot be reserved (409). */
public class MovieNotPurchasableException extends ConflictException {

    private final Long movieId;

    public MovieNotPurchasableException(Long movieId) {
        super("El producto " + movieId + " no tiene precio y no es comprable");
        this.movieId = movieId;
    }

    public Long movieId() {
        return movieId;
    }
}
