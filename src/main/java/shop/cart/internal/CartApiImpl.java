package shop.cart.internal;

import shop.cart.api.CartApi;
import shop.cart.internal.domain.CartRepository;
import shop.cart.internal.domain.CartLine;
import shop.cart.internal.domain.CartLineRepository;
import shop.cart.internal.usecase.ClearCart;
import org.springframework.stereotype.Service;

import java.util.List;

/** Module exit point: implements {@link CartApi} for {@code purchase}. */
@Service
public class CartApiImpl
        implements CartApi {

    private final CartRepository carts;
    private final CartLineRepository lines;
    private final ClearCart clear;

    public CartApiImpl(CartRepository carts, CartLineRepository lines, ClearCart clear) {
        this.carts = carts;
        this.lines = lines;
        this.clear = clear;
    }

    @Override
    public List<Long> findMovieIdsInCart(Long userId) {
        return carts.findByUserId(userId)
                .map(cart -> lines.findByCartIdOrderById(cart.getId()).stream()
                        .map(CartLine::getMovieId)
                        .toList())
                .orElse(List.of());
    }

    @Override
    public void clear(Long userId) {
        clear.execute(userId);
    }
}
