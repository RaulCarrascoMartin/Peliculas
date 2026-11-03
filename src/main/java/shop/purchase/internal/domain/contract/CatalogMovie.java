package shop.purchase.internal.domain.contract;

import java.math.BigDecimal;

/** Catalog movie as the purchase sees it: the data that gets frozen in the purchase line. */
public record CatalogMovie(Long id, String title, BigDecimal price) {
}
