package shop.purchase.internal.persistence.adapter;

import shop.cart.api.facade.CartFacade;
import shop.purchase.internal.domain.contract.UserCart;

import java.util.List;

/** Translates the purchase's {@link UserCart} contract to the public facade of {@code cart}. */
public class UserCartAdapter implements UserCart {

    private final CartFacade cart;

    public UserCartAdapter(CartFacade cart) {
        this.cart = cart;
    }

    @Override
    public List<Long> findMovieIds(Long userId) {
        return cart.findMovieIdsInCart(userId);
    }

    @Override
    public void clear(Long userId) {
        cart.clear(userId);
    }
}
