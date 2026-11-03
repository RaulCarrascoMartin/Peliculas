package shop.cart.internal.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartLineRepository extends JpaRepository<CartLine, Long> {

    List<CartLine> findByCartIdOrderById(Long cartId);

    /** One line per movie and cart (UNIQUE on cart_id + movie_id). */
    Optional<CartLine> findByCartIdAndMovieId(Long cartId, Long movieId);

    boolean existsByCartIdAndMovieId(Long cartId, Long movieId);

    /** Cleanup when a movie is removed from the catalog: removes its line from every cart. */
    void deleteByMovieId(Long movieId);

    /** Cart clearing: removes all its lines. */
    void deleteByCartId(Long cartId);
}
