package shop.cart.internal.persistence.jpa;

import shop.cart.internal.persistence.entity.CartEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartJpaRepository extends JpaRepository<CartEntity, Long> {

    /** A user's cart exists at most once (UNIQUE on user_id). */
    Optional<CartEntity> findByUserId(Long userId);
}
