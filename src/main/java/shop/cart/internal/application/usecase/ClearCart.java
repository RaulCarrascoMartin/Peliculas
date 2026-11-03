package shop.cart.internal.application.usecase;

import jakarta.transaction.Transactional;
import shop.cart.internal.domain.service.CartService;

public class ClearCart {

    private final CartService cart;

    public ClearCart(CartService cart) {
        this.cart = cart;
    }

    /** Idempotent: without a cart it does nothing. */
    @Transactional
    public void execute(Long userId) {
        cart.clear(userId);
    }
}
