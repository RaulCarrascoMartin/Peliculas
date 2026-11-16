package shop.cart.internal.domain.repository;

import shop.cart.internal.domain.dto.CartDto;
import shop.cart.internal.domain.dto.CartLineDto;
import shop.cart.internal.domain.exception.CartAlreadyExistsException;
import shop.cart.internal.domain.exception.MovieAlreadyInCartException;

import java.util.List;
import java.util.Optional;

/**
 * Persistence of the cart aggregate: the cart and its lines (children handled by their parent's repository). The
 * implementation uses JDBC and lives in {@code internal.persistence}.
 */
public interface CartRepository {

    /**
     * Inserts the cart (without id) or updates it. The update is an optimistic lock: it only applies if nobody
     * changed the cart since it was read, and increments the version; with a stale version it fails with Spring's
     * {@code OptimisticLockingFailureException} (409 in the shared handler).
     *
     * @throws CartAlreadyExistsException when inserting a second cart for the same user (UNIQUE on user_id)
     */
    CartDto save(CartDto cart);

    /** A user's cart exists at most once. */
    Optional<CartDto> findByUserId(Long userId);

    /**
     * Inserts the line and returns it with the assigned id.
     *
     * @throws MovieAlreadyInCartException if the cart already has a line of that movie (UNIQUE on cart and movie)
     */
    CartLineDto addLine(CartLineDto line);

    /** Lines of the cart in insertion order. */
    List<CartLineDto> findLines(Long cartId);

    /** One line per movie and cart. */
    boolean existsLine(Long cartId, Long movieId);

    /** Removes the line of that movie; returns {@code false} if the cart had no such line. */
    boolean removeLine(Long cartId, Long movieId);

    /**
     * Cart clearing: removes all its lines. It does not touch the cart row; the service does it afterwards with
     * {@link #save} so the version check runs.
     */
    void removeLines(Long cartId);
}
