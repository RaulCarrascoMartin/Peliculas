package shop.cart.internal.domain.dto;

import java.math.BigDecimal;

/** Cart line with the movie's title and live price (the price is never stored on the line). */
public record PricedLineDto(
        Long movieId,
        String title,
        BigDecimal price) {
}
