package shop.purchase.internal.domain.exception;

import shop.shared.error.ConflictException;

/** A movie of the cart is no longer in the catalog (409). */
public class MovieNoLongerAvailableException extends ConflictException {

    public MovieNoLongerAvailableException(Long movieId) {
        super("La película " + movieId + " ya no está en el catálogo");
    }
}
