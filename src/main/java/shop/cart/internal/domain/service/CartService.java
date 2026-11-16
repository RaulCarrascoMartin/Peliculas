package shop.cart.internal.domain.service;

import shop.cart.internal.domain.dto.CartContentsDto;
import shop.cart.internal.domain.dto.PricedLineDto;
import shop.cart.internal.domain.exception.MovieAlreadyInCartException;
import shop.cart.internal.domain.exception.MovieNotFoundException;
import shop.cart.internal.domain.exception.MovieNotInCartException;
import shop.cart.internal.domain.exception.MovieWithoutPriceException;

import java.util.List;

/** Cart rules: one line per movie, only purchasable movies, live prices and clearing. */
public interface CartService {

    /**
     * Adds the movie to the user's cart, creating the cart with the first line.
     *
     * @throws MovieNotFoundException       if the movie is not in the catalog
     * @throws MovieWithoutPriceException   if the movie has no price
     * @throws MovieAlreadyInCartException  if the movie is already in the cart
     */
    PricedLineDto addMovie(Long userId, Long movieId);

    /** @throws MovieNotInCartException if the user has no cart or the movie is not in it */
    void removeMovie(Long userId, Long movieId);

    /**
     * Lines with live price and total; an empty cart if the user has none. The price is never persisted on the
     * line: it is computed on read with the live price of the movie.
     */
    CartContentsDto contentsOf(Long userId);

    /** Ids of the movies in the cart, in insertion order. */
    List<Long> movieIdsOf(Long userId);

    /** Leaves the cart without lines; idempotent. Touches the cart so concurrent checkouts collide. */
    void clear(Long userId);
}
