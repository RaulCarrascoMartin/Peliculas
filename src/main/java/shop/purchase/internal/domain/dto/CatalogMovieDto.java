package shop.purchase.internal.domain.dto;

import java.math.BigDecimal;

/** Catalog movie as the purchase sees it: the data that gets frozen in the purchase line. */
public record CatalogMovieDto(Long id, String title, BigDecimal price) {
}
