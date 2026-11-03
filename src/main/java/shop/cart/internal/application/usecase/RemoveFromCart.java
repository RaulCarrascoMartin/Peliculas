package shop.cart.internal.application.usecase;

import jakarta.transaction.Transactional;
import shop.cart.internal.domain.service.CartService;

public class RemoveFromCart {

    private final CartService cart;

    public RemoveFromCart(CartService cart) {
        this.cart = cart;
    }

    @Transactional
    public void execute(Long userId, Long movieId) {
        cart.removeMovie(userId, movieId);
    }
}
