package shop.cart.internal.usecase;

import java.math.BigDecimal;

/** Cart line as the web sees it: with the movie's title and live price. */
public record CartLineView(
        Long movieId,
        String title,
        BigDecimal price) {
}
