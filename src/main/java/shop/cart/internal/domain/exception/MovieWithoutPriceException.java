package shop.cart.internal.domain.exception;

import shop.shared.error.ConflictException;

/** The movie has no price, so it cannot be bought (409). */
public class MovieWithoutPriceException extends ConflictException {

    public MovieWithoutPriceException() {
        super("La película no tiene precio y no es comprable");
    }
}
