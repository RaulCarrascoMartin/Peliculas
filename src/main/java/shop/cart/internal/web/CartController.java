package shop.cart.internal.web;

import shop.cart.internal.usecase.AddToCart;
import shop.cart.internal.usecase.RemoveFromCart;
import shop.cart.internal.usecase.ClearCart;
import shop.cart.internal.usecase.ViewCart;
import shop.cart.internal.usecase.CartView;
import shop.cart.internal.usecase.CartLineView;
import shop.cart.internal.web.api.CartRestApi;
import shop.cart.internal.web.dto.AddLine;
import shop.cart.internal.web.dto.Cart;
import shop.cart.internal.web.dto.CartLine;
import shop.shared.security.CurrentUser;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CartController implements CartRestApi {

    private final ViewCart viewCart;
    private final AddToCart addToCart;
    private final RemoveFromCart removeFromCart;
    private final ClearCart clearCart;
    private final CurrentUser currentUser;

    public CartController(ViewCart viewCart, AddToCart addToCart, RemoveFromCart removeFromCart,
                          ClearCart clearCart, CurrentUser currentUser) {
        this.viewCart = viewCart;
        this.addToCart = addToCart;
        this.removeFromCart = removeFromCart;
        this.clearCart = clearCart;
        this.currentUser = currentUser;
    }

    @Override
    public Cart viewCart() {
        return toDto(viewCart.execute(currentUser.id()));
    }

    @Override
    public CartLine addLine(AddLine addLine) {
        // peliculaId is the wire name from the OpenAPI contract
        CartLineView line = addToCart.execute(currentUser.id(), addLine.peliculaId());
        return toDto(line);
    }

    @Override
    public void removeLine(Long movieId) {
        removeFromCart.execute(currentUser.id(), movieId);
    }

    @Override
    public void clearCart() {
        clearCart.execute(currentUser.id());
    }

    private static Cart toDto(CartView view) {
        List<CartLine> lines = view.lines().stream()
                .map(CartController::toDto)
                .toList();
        return new Cart(lines, view.total());
    }

    private static CartLine toDto(CartLineView line) {
        return new CartLine(line.movieId(), line.title(), line.price());
    }
}
