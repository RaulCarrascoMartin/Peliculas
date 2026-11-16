package shop.cart.internal.domain.repository;

import shop.cart.internal.domain.model.Cart;
import shop.cart.internal.domain.model.CartLine;

import java.util.List;
import java.util.Optional;

public interface CartLineRepository {

    CartLine save(CartLine line);

    void delete(CartLine line);

    List<CartLine> findByCartIdOrderById(Long cartId);

    /** One line per movie and cart. */
    Optional<CartLine> findByCartIdAndMovieId(Long cartId, Long movieId);

    boolean existsByCartIdAndMovieId(Long cartId, Long movieId);

    /** Cleanup when a movie is removed from the catalog: removes its line from every cart. */
    void deleteByMovieId(Long movieId);

    /** Cart clearing: removes all its lines. */
    void deleteByCartId(Long cartId);
}
