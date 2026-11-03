package shop.catalog.api;

import java.math.BigDecimal;

/** Public data of a movie for other modules; avoids exposing the entity. */
public record MovieSummary(
        Long id,
        String title,
        Integer year,
        String genre,
        String director,
        BigDecimal price,
        Double averageRating,
        int stock) {
}
