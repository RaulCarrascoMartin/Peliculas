package shop.cart.internal.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /** A user's cart exists at most once (UNIQUE on user_id). */
    Optional<Cart> findByUserId(Long userId);
}
