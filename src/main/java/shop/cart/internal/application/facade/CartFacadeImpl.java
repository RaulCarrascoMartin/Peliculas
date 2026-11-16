package shop.cart.internal.application.facade;

import shop.cart.internal.application.usecase.ClearCart;

import shop.cart.api.facade.CartFacade;
import shop.cart.internal.domain.service.CartService;

import java.util.List;

/** Module exit point: implements {@link CartFacade} for {@code purchase}. */
public class CartFacadeImpl implements CartFacade {

    private final CartService cart;
    private final ClearCart clear;

    public CartFacadeImpl(CartService cart, ClearCart clear) {
        this.cart = cart;
        this.clear = clear;
    }

    @Override
    public List<Long> findMovieIdsInCart(Long userId) {
        return cart.movieIdsOf(userId);
    }

    @Override
    public void clear(Long userId) {
        clear.execute(userId);
    }
}
