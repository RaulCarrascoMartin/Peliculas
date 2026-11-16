package shop.cart.internal.application.usecase;

import shop.cart.internal.application.dto.CartLineView;

import jakarta.transaction.Transactional;
import shop.cart.internal.domain.service.CartService;

public class AddToCart {

    private final CartService cart;

    public AddToCart(CartService cart) {
        this.cart = cart;
    }

    @Transactional
    public CartLineView execute(Long userId, Long movieId) {
        CartService.PricedLine line = cart.addMovie(userId, movieId);
        return new CartLineView(line.movieId(), line.title(), line.price());
    }
}
