package shop.catalog.internal.domain.exception;

import shop.shared.error.ConflictException;

/** There are not enough units of the movie to fulfil the reservation (409). */
public class InsufficientStockException extends ConflictException {

    private final Long movieId;

    public InsufficientStockException(Long movieId) {
        super("No hay stock suficiente del producto " + movieId);
        this.movieId = movieId;
    }

    public Long movieId() {
        return movieId;
    }
}
