package shop.cart.internal.usecase;

import shop.cart.internal.domain.CartRepository;
import shop.cart.internal.domain.CartLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ClearCart {

    private final CartRepository carts;
    private final CartLineRepository lines;

    public ClearCart(CartRepository carts, CartLineRepository lines) {
        this.carts = carts;
        this.lines = lines;
    }

    /** Idempotent: without a cart it does nothing. */
    @Transactional
    public void execute(Long userId) {
        carts.findByUserId(userId).ifPresent(cart -> {
            lines.deleteByCartId(cart.getId());
            // Real change of the entity: forces the update with @Version, which makes the second simultaneous checkout lose.
            cart.setUpdatedAt(LocalDateTime.now());
            carts.save(cart);
        });
    }
}
