package shop.cart.internal.domain.service;

import shop.cart.internal.domain.exception.MovieAlreadyInCartException;
import shop.cart.internal.domain.exception.MovieNotFoundException;
import shop.cart.internal.domain.exception.MovieNotInCartException;
import shop.cart.internal.domain.exception.MovieWithoutPriceException;
import shop.cart.internal.domain.model.Cart;

import java.math.BigDecimal;
import java.util.List;

/** Cart rules: one line per movie, only purchasable movies, live prices and clearing. */
public interface CartService {

    /** Cart line with the movie's title and live price (the price is never stored on the line). */
    record PricedLine(Long movieId, String title, BigDecimal price) {
    }

    /** Current state of a user's cart: lines with live price and total. */
    record CartContents(List<PricedLine> lines, BigDecimal total) {
    }

    /**
     * Adds the movie to the user's cart, creating the cart with the first line.
     *
     * @throws MovieNotFoundException       if the movie is not in the catalog
     * @throws MovieWithoutPriceException   if the movie has no price
     * @throws MovieAlreadyInCartException  if the movie is already in the cart
     */
    PricedLine addMovie(Long userId, Long movieId);

    /** @throws MovieNotInCartException if the user has no cart or the movie is not in it */
    void removeMovie(Long userId, Long movieId);

    /** Lines with live price and total; an empty cart if the user has none. */
    CartContents contentsOf(Long userId);

    /** Ids of the movies in the cart, in insertion order. */
    List<Long> movieIdsOf(Long userId);

    /** Leaves the cart without lines; idempotent. Touches the cart so concurrent checkouts collide. */
    void clear(Long userId);
}
