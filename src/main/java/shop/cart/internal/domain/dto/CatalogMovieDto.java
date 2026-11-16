package shop.cart.internal.domain.dto;

import java.math.BigDecimal;

/** Catalog movie as the cart sees it: title and live price ({@code null} if it is not purchasable). */
public record CatalogMovieDto(
        Long id,
        String title,
        BigDecimal price) {
}
