package shop.cart.api.http.controller;

import shop.cart.api.http.CartHttpApi;
import shop.cart.api.http.mapper.CartWebMapper;

import shop.cart.internal.application.usecase.AddToCart;
import shop.cart.internal.application.usecase.RemoveFromCart;
import shop.cart.internal.application.usecase.ClearCart;
import shop.cart.internal.application.usecase.ViewCart;
import shop.cart.internal.application.dto.CartLineView;
import shop.cart.api.http.dto.AddLine;
import shop.cart.api.http.dto.Cart;
import shop.cart.api.http.dto.CartLine;
import shop.shared.security.CurrentUser;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CartController implements CartHttpApi {

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
        return CartWebMapper.toDto(viewCart.execute(currentUser.id()));
    }

    @Override
    public CartLine addLine(AddLine addLine) {
        // peliculaId is the wire name from the OpenAPI contract
        CartLineView line = addToCart.execute(currentUser.id(), addLine.peliculaId());
        return CartWebMapper.toDto(line);
    }

    @Override
    public void removeLine(Long movieId) {
        removeFromCart.execute(currentUser.id(), movieId);
    }

    @Override
    public void clearCart() {
        clearCart.execute(currentUser.id());
    }
}
