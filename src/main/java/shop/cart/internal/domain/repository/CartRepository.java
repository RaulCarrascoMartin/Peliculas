package shop.cart.internal.domain.repository;

import shop.cart.internal.domain.exception.CartAlreadyExistsException;
import shop.cart.internal.domain.model.Cart;

import java.util.Optional;

public interface CartRepository {

    /** @throws CartAlreadyExistsException when inserting a second cart for the same user (UNIQUE on user_id) */
    Cart save(Cart cart);

    /** A user's cart exists at most once. */
    Optional<Cart> findByUserId(Long userId);
}
