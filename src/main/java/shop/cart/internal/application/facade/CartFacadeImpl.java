package shop.cart.internal.application.facade;

import org.springframework.stereotype.Component;
import shop.cart.api.facade.CartFacade;
import shop.cart.internal.domain.service.CartService;

import java.util.List;

/** Module exit point: implements {@link CartFacade} for {@code purchase} by delegating to the service. */
@Component
public class CartFacadeImpl implements CartFacade {

    private final CartService cart;

    public CartFacadeImpl(CartService cart) {
        this.cart = cart;
    }

    @Override
    public List<Long> findMovieIdsInCart(Long userId) {
        return cart.movieIdsOf(userId);
    }

    @Override
    public void clear(Long userId) {
        cart.clear(userId);
    }
}
