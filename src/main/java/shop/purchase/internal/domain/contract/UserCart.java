package shop.purchase.internal.domain.contract;

import java.util.List;

/** What the purchase needs from the cart: read its movies and clear it. An adapter in {@code persistence} fulfils it. */
public interface UserCart {

    /** Ids of the movies in the user's cart, in insertion order. */
    List<Long> findMovieIds(Long userId);

    /** Leaves the user's cart without lines; idempotent. */
    void clear(Long userId);
}
