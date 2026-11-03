package shop.cart.internal.usecase;

import shop.cart.internal.domain.Cart;
import shop.cart.internal.domain.CartRepository;
import shop.cart.internal.domain.CartLine;
import shop.cart.internal.domain.CartLineRepository;
import shop.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemoveFromCart {

    private final CartRepository carts;
    private final CartLineRepository lines;

    public RemoveFromCart(CartRepository carts, CartLineRepository lines) {
        this.carts = carts;
        this.lines = lines;
    }

    @Transactional
    public void execute(Long userId, Long movieId) {
        Cart cart = carts.findByUserId(userId)
                .orElseThrow(() -> notInCart(movieId));
        CartLine line = lines.findByCartIdAndMovieId(cart.getId(), movieId)
                .orElseThrow(() -> notInCart(movieId));
        lines.delete(line);
    }

    private static ResourceNotFoundException notInCart(Long movieId) {
        return new ResourceNotFoundException("La película " + movieId + " no está en el carrito");
    }
}
