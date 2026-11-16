package shop.cart.api.http.controller;

import org.springframework.web.bind.annotation.RestController;
import shop.cart.api.http.CartHttpApi;
import shop.cart.api.http.dto.AddCartLineRequest;
import shop.cart.api.http.dto.CartLineResponse;
import shop.cart.api.http.dto.CartResponse;
import shop.cart.api.http.mapper.CartWebMapper;
import shop.cart.internal.domain.service.CartService;
import shop.shared.security.CurrentUser;

/**
 * HTTP entry of the cart: implements the contract generated from {@code carrito-api.yml} and delegates. The user
 * is always the one of the token.
 */
@RestController
public class CartController implements CartHttpApi {

    private final CartService cartService;
    private final CartWebMapper mapper;
    private final CurrentUser currentUser;

    public CartController(CartService cartService, CartWebMapper mapper, CurrentUser currentUser) {
        this.cartService = cartService;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @Override
    public CartResponse viewCart() {
        return mapper.toResponse(cartService.contentsOf(currentUser.id()));
    }

    @Override
    public CartLineResponse addLine(AddCartLineRequest addCartLineRequest) {
        // peliculaId is the wire name from the OpenAPI contract
        return mapper.toResponse(cartService.addMovie(currentUser.id(), addCartLineRequest.peliculaId()));
    }

    @Override
    public void removeLine(Long peliculaId) {
        cartService.removeMovie(currentUser.id(), peliculaId);
    }

    @Override
    public void clearCart() {
        cartService.clear(currentUser.id());
    }
}
