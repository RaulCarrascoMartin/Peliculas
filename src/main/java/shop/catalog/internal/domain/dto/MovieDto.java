package shop.catalog.internal.domain.dto;

import java.math.BigDecimal;

/**
 * Movie as it travels between layers: controller, service and repository. {@code averageRating} is {@code null}
 * while there are no ratings and {@code price} only for movies that predate the price (not purchasable).
 */
public record MovieDto(
        Long id,
        String title,
        Integer year,
        String genre,
        String director,
        BigDecimal price,
        Double averageRating,
        int stock) {
}
