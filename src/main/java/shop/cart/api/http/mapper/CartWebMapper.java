package shop.cart.api.http.mapper;

import shop.cart.api.http.dto.Cart;
import shop.cart.api.http.dto.CartLine;
import shop.cart.internal.application.dto.CartLineView;
import shop.cart.internal.application.dto.CartView;

import java.util.List;

/** Converts the use case views into the DTO of the HTTP contract. */
public final class CartWebMapper {

    private CartWebMapper() {
    }

    public static Cart toDto(CartView view) {
        List<CartLine> lines = view.lines().stream()
                .map(CartWebMapper::toDto)
                .toList();
        return new Cart(lines, view.total());
    }

    public static CartLine toDto(CartLineView line) {
        return new CartLine(line.movieId(), line.title(), line.price());
    }
}
