package shop.purchase.internal.domain.exception;

import shop.shared.error.ConflictException;

/** A movie of the cart has no price, so it cannot be bought (409). */
public class MovieWithoutPriceException extends ConflictException {

    public MovieWithoutPriceException(Long movieId) {
        super("La película " + movieId + " no tiene precio y no es comprable");
    }
}
