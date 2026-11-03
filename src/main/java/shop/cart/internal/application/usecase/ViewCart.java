package shop.cart.internal.application.usecase;

import shop.cart.internal.application.dto.CartLineView;
import shop.cart.internal.application.dto.CartView;

import shop.cart.internal.domain.service.CartService;

/** The price is never persisted on the line: it is computed on read with the live price of the movie. */
public class ViewCart {

    private final CartService cart;

    public ViewCart(CartService cart) {
        this.cart = cart;
    }

    public CartView execute(Long userId) {
        CartService.CartContents contents = cart.contentsOf(userId);
        return new CartView(
                contents.lines().stream()
                        .map(l -> new CartLineView(l.movieId(), l.title(), l.price()))
                        .toList(),
                contents.total());
    }
}
