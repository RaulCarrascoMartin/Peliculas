package shop.cart.api;

import java.util.List;

/** Public API of the {@code cart} module: the only thing other modules (purchase) may use. */
public interface CartApi {

    /** Ids of the movies in the user's cart, in insertion order. */
    List<Long> findMovieIdsInCart(Long userId);

    /** Leaves the user's cart without lines; idempotent. */
    void clear(Long userId);
}
